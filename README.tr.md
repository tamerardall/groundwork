<p align="center"><img src="assets/logo.svg" width="96" alt="groundwork logosu"></p>

# groundwork

AI kodlama ajanlarının düzgün yapılandırılmış kod üretmesini ve kodu, yaptığı işi değiştirmeden hızlandırmasını sağlayan skill'ler.

[English](./README.md) | Türkçe

## Sorun

AI ajanları çalışan ama yapısı kötü kod yazıyor:

- **Her şey tek dosyada.** Koca bir DLNA cast aracı (keşif, SOAP çağrıları, HTTP sunucusu, CLI) tek bir `main.py` dosyasına yazılıyor.
- **Katmanlar iç içe.** Spring Boot controller'ı ve service'i aynı `.java` dosyasında duruyor.
- **Tek kullanımlık helper'lar.** Yalnızca bir yerden çağrılan üç satırlık private metotlar okuyucuyu boşuna başka yere zıplatıyor.
- **`utils` çöplükleri.** Domain kodu `helpers.py` ya da `CommonUtils.java` içine saklanıyor.
- **Güvensiz "optimizasyonlar".** Bir Spring servisini hızlandırması istenen ajan satırların sırasını değiştiriyor ve istemciye dönen hatayı sessizce değiştiriyor. Asıl kazancı ise atlıyor: transaction'ı açık tutan uzak çağrı.

Bu skill'ler ajana açık ve referans verilebilir kurallar (`S-01`, `J-03`, ...) veriyor. Böylece kod en baştan doğru yapılandırılıyor. Mevcut kodu düzeltmek için de bir review akışı var.

## Skill'ler

| Skill | Ne zaman yüklenir | Ne sağlar |
|---|---|---|
| [`structure-rules`](skills/structure-rules/SKILL.md) | Yeni kod yazarken, modül eklerken, helper çıkarırken | Dilden bağımsız S-01–S-09 kuralları (dosya ve katman sınırları, tek kullanımlık helper, tekrar kod, hata yönetimi, isimlendirme, spekülatif kod) ve teslim kontrolü |
| [`java-package-structure`](skills/java-package-structure/SKILL.md) | Java / Spring Boot işlerinde | Modül bazlı paket yapısı, J-01–J-11 kuralları (null safety dahil), [önce/sonra örneği](skills/java-package-structure/examples.md) |
| [`structure-review`](skills/structure-review/SKILL.md) | Mevcut kodu incelerken veya refactor ederken | Tarama → rapor → davranışı bozmadan adım adım düzeltme |
| [`spring-service-ordering`](skills/spring-service-ordering/SKILL.md) | Spring Boot / JPA servis metotlarını optimize ederken ya da incelerken | O-01–O-06 kalıpları (sorguyu erteleme, ucuz kontrolü öne alma, transaction daraltma, döngüyü toplu sorguya çevirme, paralel uzak çağrı), B1–B11 bariyerleri, `safe` / `business-decision` / `suspect` risk seviyeleri, rapor → onay → characterization test kapısı. [Kalıplar](skills/spring-service-ordering/patterns.md) · [Bariyerler](skills/spring-service-ordering/safety.md) · [Örnekler](skills/spring-service-ordering/examples.md) |
| [`sql-query-tuning`](skills/sql-query-tuning/SKILL.md) | Yavaş bir SQL / JPQL / native sorguyu hızlandırırken ya da incelerken | Önce motoru, index'leri (kolon sırasıyla), satır sayılarını ve `EXPLAIN` çıktısını sorar. Q-01–Q-08 kalıpları (sargable koşul, composite index hizalama, `NOT IN` → `NOT EXISTS`, tekrarlanan alt sorgu → CTE, eksik index), X1–X8 bariyerleri, rapor → onay → sonuç kümesi ve `EXPLAIN` karşılaştırması. Efsanelere dürüst: WHERE sırası ve düz `WITH` çoğu motorda hiçbir şeyi değiştirmez. [Kalıplar](skills/sql-query-tuning/patterns.md) · [Motorlar](skills/sql-query-tuning/engines.md) · [Örnekler](skills/sql-query-tuning/examples.md) |

### Java paket yapısı

```
com.company.project
├── ProjectApplication.java
├── common/{config, exception, security, util}
├── user/{controller, service, repository, model/entity, model/dto, mapper}
└── product/{controller, service, repository, model/entity, model/dto, mapper}
```

Her modülün altında yalnızca o modülü ilgilendiren class'lar bulunur. Modüller birbirine yalnızca service arayüzleri üzerinden erişir.

## Değerlendirme: spring-service-ordering

`tests/fixture` 14 servis metodu içeren bir Spring Boot 4 projesi. Metotların 7'si düzeltilmeli, 7'si tuzak. Tuzaklar şunları sınıyor: auto-flush, rollback, `this.` çağrıları, SecurityContext, zaman damgaları ve sınıf seviyesinde transaction. Cevap anahtarı [`tests/EXPECTED.md`](tests/EXPECTED.md) dosyasında.

| Ölçüt | Skill'siz | Skill'le |
|---|---|---|
| Cevap anahtarıyla uyuşan metot | 11 / 14 | 14 / 14 |
| Sessizce verilen iş kararı | 1 | 0 |
| Transaction daraltma bulgusu (O-04) | 4'te 0 | 4'te 4 |
| Onay için durma; test kapısı olmadan düzenlememe | — | evet |

Ayrıntılar: [`tests/results/`](tests/results/).

## Kurulum

### Claude Code (plugin)

```
/plugin marketplace add tamerardall/groundwork
/plugin install groundwork@groundwork
```

Ya da repoyu klonlayıp istediğin skill'leri symlink'le:

```bash
git clone https://github.com/tamerardall/groundwork.git
ln -s "$PWD/groundwork/skills/structure-rules" ~/.claude/skills/structure-rules
```

### Codex, Copilot, Gemini CLI, Cursor vb.

[`AGENTS.md`](AGENTS.md) dosyasını projenin köküne kopyala ya da mevcut `AGENTS.md` dosyanın sonuna ekle. Tüm kuralların özet hali bu dosyada.

## Birlikte iyi çalışır

- Genel LLM kodlama davranışı için [karpathy-guidelines](https://github.com/forrestchang/andrej-karpathy-skills)
- `antislop-code` gibi yorum hijyeni skill'leri

groundwork **yapı** (dosya, paket ve fonksiyon sınırları) ve **davranışı aynen koruyan performans** ile ilgilenir. SQL ayarı yapmaz, cache eklemez, API değiştirmez.

## Katkı

Başka diller (Python, TypeScript, Go, ...) için kurallar memnuniyetle karşılanır. Bkz. [CONTRIBUTING.md](CONTRIBUTING.md).

## Lisans

MIT
