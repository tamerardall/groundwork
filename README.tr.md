# unslop-skills

AI kodlama ajanlarının düzgün yapılandırılmış kod üretmesini sağlayan skill'ler.

[English](./README.md) | Türkçe

## Sorun

AI ajanları çalışan ama yapısı kötü kod yazıyor:

- **Her şey tek dosyada.** Koca bir DLNA cast aracı (keşif, SOAP çağrıları, HTTP sunucusu, CLI) tek bir `main.py` dosyasına yazılıyor.
- **Katmanlar iç içe.** Spring Boot controller'ı ve service'i aynı `.java` dosyasında duruyor.
- **Tek kullanımlık helper'lar.** Yalnızca bir yerden çağrılan üç satırlık private metotlar okuyucuyu boşuna başka yere zıplatıyor.
- **`utils` çöplükleri.** Domain kodu `helpers.py` ya da `CommonUtils.java` içine saklanıyor.

Bu skill'ler ajana açık ve referans verilebilir kurallar (`S-01`, `J-03`, ...) veriyor. Böylece kod en baştan doğru yapılandırılıyor. Mevcut kodu düzeltmek için de bir review akışı var.

## Skill'ler

| Skill | Ne zaman yüklenir | Ne sağlar |
|---|---|---|
| [`structure-rules`](skills/structure-rules/SKILL.md) | Yeni kod yazarken, modül eklerken, helper çıkarırken | Dilden bağımsız S-01–S-09 kuralları (dosya ve katman sınırları, tek kullanımlık helper, tekrar kod, hata yönetimi, isimlendirme, spekülatif kod) ve teslim kontrolü |
| [`java-package-structure`](skills/java-package-structure/SKILL.md) | Java / Spring Boot işlerinde | Modül bazlı paket yapısı, J-01–J-11 kuralları (null safety dahil), [önce/sonra örneği](skills/java-package-structure/examples.md) |
| [`structure-review`](skills/structure-review/SKILL.md) | Mevcut kodu incelerken veya refactor ederken | Tarama → rapor → davranışı bozmadan adım adım düzeltme |

### Java paket yapısı

```
com.company.project
├── ProjectApplication.java
├── common/{config, exception, security, util}
├── user/{controller, service, repository, model/entity, model/dto, mapper}
└── product/{controller, service, repository, model/entity, model/dto, mapper}
```

Her modülün altında yalnızca o modülü ilgilendiren class'lar bulunur. Modüller birbirine yalnızca service arayüzleri üzerinden erişir.

## Kurulum

### Claude Code (plugin)

```
/plugin marketplace add tamerardall/unslop-skills
/plugin install unslop-skills@unslop-skills
```

Ya da repoyu klonlayıp istediğin skill'leri symlink'le:

```bash
git clone https://github.com/tamerardall/unslop-skills.git
ln -s "$PWD/unslop-skills/skills/structure-rules" ~/.claude/skills/structure-rules
```

### Codex, Copilot, Gemini CLI, Cursor vb.

[`AGENTS.md`](AGENTS.md) dosyasını projenin köküne kopyala ya da mevcut `AGENTS.md` dosyanın sonuna ekle. Tüm kuralların özet hali bu dosyada.

## Birlikte iyi çalışır

- Genel LLM kodlama davranışı için [karpathy-guidelines](https://github.com/forrestchang/andrej-karpathy-skills)
- `antislop-code` gibi yorum hijyeni skill'leri

unslop-skills yalnızca **yapı** ile ilgilenir: dosya, paket ve fonksiyon sınırları.

## Katkı

Başka diller (Python, TypeScript, Go, ...) için kurallar memnuniyetle karşılanır. Bkz. [CONTRIBUTING.md](CONTRIBUTING.md).

## Lisans

MIT
