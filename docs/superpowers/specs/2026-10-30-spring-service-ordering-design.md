# Tasarım: `spring-service-ordering` skill'i (yeni repo: `perf-skills`)

## Context

Spring Boot servis metotlarında işlem sırası çoğu zaman performansı boşa harcıyor:
- DB çağrısı metodun başında yapılıyor ama sonuç erken çıkış yolunda hiç kullanılmıyor.
- Bir nesne en başta oluşturuluyor ama metodun sonunda kullanılıyor.
- Harici HTTP çağrısı `@Transactional` içinde kalıp DB bağlantısını tutuyor.

Amaç bu sorunları bulan ve düzeltme öneren bir agent skill'i yazmak. Kesin kısıt şu: **iş kuralları bozulmayacak.** Sıra değişikliği gözlemlenen davranışı değiştiriyorsa (hangi hata önce fırlıyor, hangi yan etki önce oluyor, ne zaman flush ediliyor) skill o değişikliği önermez ya da açıkça "iş kararı" olarak işaretler.

**Kullanıcının kararları:**
- Hedef: Java / Spring Boot.
- Çıktı: Skill önce öneri raporu sunar, kullanıcı onaylarsa uygular.
- Kapsam: Altı kalıp (aşağıda).
- Güvence: Analiz + test kapısı. Test yoksa skill önce mevcut davranışı sabitleyen bir characterization testi yazar.
- Konum: unslop-skills değil, **yeni ayrı bir repo**.

**Varsayımlar (düzeltilebilir):**
- Repo adı `perf-skills`, yeri `~/Projeler/perf-skills`.
- Skill adı `spring-service-ordering`.
- Skill içeriği İngilizce, README hem İngilizce hem Türkçe (unslop-skills ile aynı yaklaşım).
- Repo yerelde `git init` ile başlar. GitHub'a push ancak ayrıca istenirse yapılır.

## Repo düzeni (unslop-skills kalıbını izler)

```
~/Projeler/perf-skills/
├── README.md, README.tr.md, LICENSE (MIT), AGENTS.md (özet kurallar)
├── .claude-plugin/plugin.json
├── skills/spring-service-ordering/
│   ├── SKILL.md        # tetikleyici, iş akışı, kural tablosu, rapor formatı (<500 satır)
│   ├── patterns.md     # O-01..O-06: Tell / Why / Fix + güvenli olma koşulları
│   ├── safety.md       # "Asla yer değiştirme" listesi (bariyerler)
│   └── examples.md     # her kalıp için önce/sonra + bir tuzak örneği
└── tests/fixture/      # skill'i sınamak için küçük Spring servis örnekleri (bkz. Doğrulama)
```

`~/.claude/skills/spring-service-ordering` symlink'i repodaki klasöre bağlanır (java-package-structure ile aynı yöntem).

## Kalıplar (patterns.md)

| ID | Kalıp | Güvenli olma koşulu |
|----|-------|---------------------|
| O-01 | Sonucu sonradan kullanılan DB/uzak çağrıyı ilk kullanıldığı yere taşı, erken çıkış yolunda hiç çağırma | Arada yazma ya da flush yok, çağrının kendi yan etkisi yok, exception sırası korunuyor |
| O-02 | Erken oluşturulan nesneyi kullanıldığı yere taşı | Oluşturma işinin yan etkisi yok (zaman damgası, ID üretimi, sayaç) |
| O-03 | Ucuz kontrolü (validasyon, null, yetki) pahalı çağrıdan önce yap | Kontrol, pahalı çağrının sonucuna bağlı değil. **Birden fazla hata aynı anda olabiliyorsa hangisinin döneceği değişir. Bu bir iş kararıdır, işaretlenir.** |
| O-04 | Transaction kapsamını daralt: harici IO'yu tx dışına ya da `afterCommit` / `@TransactionalEventListener(AFTER_COMMIT)`'e taşı | IO'nun başarısızlığı tx'i rollback ettirmek zorunda değil (iş kuralı kontrol edilir), self-invocation proxy tuzağı yok |
| O-05 | Döngüdeki sorguyu tek bir toplu sorguya çevir (`findAllById`, `IN`, fetch join, `@EntityGraph`) | Döngüde her elemanın bulunamaması özel bir davranış tetiklemiyor ya da bu davranış korunuyor, sıralama ve tekrar eden ID'ler ele alınıyor |
| O-06 | Bağımsız uzak çağrıları `CompletableFuture` ile paralel yap | **En sıkı kurallar:** DB/tx içermez, SecurityContext/MDC/tenant ThreadLocal'a dayanmaz (ya da bunlar aktarılır), özel executor kullanılır, hata semantiği korunur |

## Bariyerler (safety.md): iki ifade bu durumlarda yer değiştirmez

- İkisinden biri yazıyor (save, delete, update, event publish, mesaj, e-posta, log/audit), diğeri aynı veriyi okuyor ya da yazıyor.
- JPA auto-flush: JPQL/Criteria sorgusu, bekleyen değişikliklerin flush edilmesine bağlı.
- Lazy loading: Taşınan erişim tx/session sınırının dışına düşüyor.
- Persistence context kimliği: Aynı entity'nin iki kez okunması önbellekten döner. Sıra, hangi durumun görüleceğini değiştirir.
- Optimistic locking (`@Version`), `SELECT ... FOR UPDATE` ve kilit sırası (deadlock riski).
- Yetki kontrolü veri açığa çıkmadan önce kalmalı. Güvenlik kontrolü asla geri alınmaz.
- Exception sırası istemciye dönen hata kodunu ya da mesajını değiştiriyor.
- `this.` ile çağrılan `@Transactional` / `@Async` / `@Cacheable` metodu (proxy atlanıyor).
- ThreadLocal bağlamı: SecurityContext, MDC, TenantContext, `RequestContextHolder`.
- Zaman ve kimlik üretimi: `now()`, `UUID`, sequence. Sıra değişirse değerler değişir.

Belirsiz durumda varsayılan karar: **önerme, "şüpheli" olarak raporla.**

## İş akışı (SKILL.md)

1. **Kapsam:** Kullanıcının verdiği metot, sınıf ya da diff. Belirtilmemişse sorar, tüm projeyi kendiliğinden taramaz.
2. **Haritalama:** Metottaki her ifade için bir tablo çıkarır: okur / yazar / yan etki / fırlatabilir / tx içinde mi / bağımlılıkları.
3. **Eşleştirme:** Kalıplar (O-01..O-06) ile eşleştirir. Her aday için bariyer listesini kontrol eder.
4. **Rapor ve durma:** Her bulgu için kalıp ID'si, önce/sonra kodu, beklenen kazanç (ör. "erken çıkış yolunda 1 sorgu daha az") ve güvenlik gerekçesi verir. Ayrıca risk seviyesini yazar: güvenli / iş kararı / şüpheli (önerilmez). **Burada durur ve onay bekler.**
5. **Test kapısı:** Onaylanan her metot için mevcut testlere bakar. Test yoksa davranışı sabitleyen bir characterization testi yazar. Bu test çağrı sırasını değil sonucu doğrular: dönüş değeri, fırlatılan exception, yan etki çağrıları ve argümanları. Test **eski kodda yeşil** olmalı.
6. **Uygulama:** Her seferinde tek bir kalıbı uygular, ardından testi çalıştırır. Test kırmızıya dönerse değişikliği geri alır ve raporlar.
7. **Son rapor:** Ne uygulandı, ne atlandı ve neden.

Rapor formatı ve kural tablosu SKILL.md'de bulunur. Ayrıntılar referans dosyalarındadır (progressive disclosure).

## Uygulama yöntemi

- Skill yazımı `superpowers:writing-skills` skill'iyle yapılır: önce baseline (skill olmadan agent ne yapıyor), sonra skill ve iyileştirme turu.
- Sıra: önce fixture servisleri ve baseline, sonra SKILL.md → patterns.md → safety.md → examples.md, sonra README, AGENTS.md ve plugin.json, en son symlink.

## Doğrulama

1. **Fixture:** `tests/fixture/` altında küçük Spring servis sınıfları bulunur. Her kalıp için bir "düzeltilmeli" örneği ve bir **tuzak** örneği vardır. Tuzak, düzeltilmiş gibi görünen ama bariyere takılan koddur (ör. auto-flush'a bağlı sorgu, iş kuralı olan validasyon sırası, `this.` çağrısı).
2. **Baseline:** Skill olmadan bir subagent'a "bu servisleri performans için yeniden sırala" denir ve hataları kaydedilir.
3. **Skill ile:** Aynı görev tekrarlanır. Başarı ölçütü: altı kalıbın hepsi bulunur, tuzakların hiçbiri "güvenli" önerilmez, O-03 ve O-06 doğru seviyede işaretlenir ve agent onay noktasında durur.
4. **Test kapısı:** Bir fixture'ı uygulama moduna geçirip characterization testinin eski kodda yeşil olduğu, değişiklikten sonra da yeşil kaldığı doğrulanır. Fixture derleniyorsa `mvn test` ile.
5. `SKILL.md` 500 satırın altında olmalı ve `~/.claude/skills` symlink'i üzerinden skill listesinde görünmeli.
