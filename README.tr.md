# perf-skills

Yapay zekâ kod asistanlarının kodu, yaptığı işi değiştirmeden hızlandırmasını sağlayan skill'ler.

[English](./README.md) | Türkçe

## Sorun

Spring servis metotları çoğu zaman işi yanlış yerde yapar:

- Metodun başındaki bir repository çağrısının sonucuna ancak erken bir `return`'den sonra ihtiyaç duyulur.
- HTTP, e-posta ya da ödeme çağrısı `@Transactional` içinde kalır ve DB bağlantısını çağrı süresince tutar.
- Döngü içinde `findById` çağrılır.
- Birbirinden bağımsız iki uzak çağrı arka arkaya yapılır.

"Optimize et" denince agent bunların bir kısmını bulur. Ama iş kurallarına kendi başına karar verir ("yeni hata sırası daha doğru"). Transaction sınırı düzeltmelerini "sıra değişikliği değil" diye atlar. Mevcut davranışı bir testle sabitlemeden kodu değiştirir.

## Skill'ler

| Skill | Ne zaman yüklenir | Ne sağlar |
|---|---|---|
| [`spring-service-ordering`](skills/spring-service-ordering/SKILL.md) | Spring Boot / JPA servis metotlarını optimize ederken ya da incelerken | O-01–O-06 kalıpları, B1–B11 bariyerleri, üç risk seviyesi, önce rapor ve onay, ardından characterization test kapısı. [Kalıplar](skills/spring-service-ordering/patterns.md) · [Bariyerler](skills/spring-service-ordering/safety.md) · [Örnekler](skills/spring-service-ordering/examples.md) |

| ID | Kalıp |
|---|---|
| O-01 | Sorguyu ya da uzak çağrıyı ilk kullanıldığı yere taşı |
| O-02 | Nesneyi kullanıldığı yerde oluştur |
| O-03 | Ucuz kontrolü pahalı işten önce yap |
| O-04 | Uzak çağrıların etrafındaki transaction'ı daralt |
| O-05 | Döngüdeki sorguyu tek bir toplu sorguya çevir |
| O-06 | Bağımsız uzak çağrıları paralel yap |

Her bulgu üç seviyeden birini alır:
- `safe`
- `business-decision`: bazı girdilerin sonucu değişir, kararı kullanıcı verir.
- `suspect (not proposed)`

## Değerlendirme

`tests/fixture` 14 servis metodu içeren bir Spring Boot 4 projesi. Metotların 7'si düzeltilmeli, 7'si tuzak. Tuzaklar şunları sınıyor: auto-flush, rollback, `this.` çağrıları, SecurityContext, zaman damgaları ve sınıf seviyesinde transaction. Cevap anahtarı [`tests/EXPECTED.md`](tests/EXPECTED.md) dosyasında.

| Ölçüt | Skill'siz | Skill'le |
|---|---|---|
| Cevap anahtarıyla uyuşan metot | 11 / 14 | 14 / 14 |
| Sessizce verilen iş kararı | 1 | 0 |
| Transaction daraltma bulgusu (O-04) | 4'te 0 | 4'te 4 |
| Onay için durma; test kapısı olmadan düzenlememe | — | evet |

Ayrıntılar: [`tests/results/`](tests/results/).

## Kurulum

Repoyu klonlayıp skill'i symlink ile bağla:

```bash
git clone <repo-url> perf-skills
ln -s "$PWD/perf-skills/skills/spring-service-ordering" ~/.claude/skills/spring-service-ordering
```

Ya da yerel klondan Claude Code plugin'i olarak kur:

```
/plugin marketplace add ./perf-skills
/plugin install perf-skills@perf-skills
```

Codex, Copilot, Gemini CLI, Cursor ve diğerleri için [`AGENTS.md`](AGENTS.md) dosyasını projene kopyala.

## Birlikte iyi çalışır

Kod yapısı için [unslop-skills](https://github.com/tamerardall/unslop-skills). perf-skills yalnızca **davranışı aynen koruyan performans** işleriyle ilgilenir.

## Lisans

MIT
