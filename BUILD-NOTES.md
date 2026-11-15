# Citizens2 — Yerel Build Notları

Son güncelleme: 2026-08-01 · Hedef: **Minecraft 26.1.2**

> **15 Kasım 2026, `projects-engine` dalı: bu dosyadaki "yalnız 26.1.2" kararının yerini aldı.** Tek jar hem
> zephyr'ı (26.1.2) hem Aurora'yı (26.2) desteklesin diye `v26_2_R1` modülü yeniden derlemede (`pom.xml` ve
> `dist/pom.xml`, `spigot-release` profili), `main/pom.xml` → `craftbukkit.version` yine upstream'deki gibi
> `26.2-R0.1-SNAPSHOT` ve `SulfurCubeTrait.java` geri geldi (`v26_2_R1`'in `NMSImpl`'i onu kaydediyor; upstream da
> `main`'i 26.2'ye karşı derleyip 26.1'de çalıştırıyor). Yalnız `v1_21_R7` dışarıda kaldı (1.21.11 remapped jar'ı
> `~/.m2`'de yok). İki Spigot jar'ı da (`26.1.2-R0.1-SNAPSHOT`, `26.2-R0.1-SNAPSHOT`) BuildTools ile kurulmuş durumda.
> Komut aynı, `-o` ile (CitizensAPI uzak snapshot'tan geliyor, çevrimdışı derleme aynı snapshot'ı kullanır):
> `mvn -o -B clean package -DskipTests -Dmaven.javadoc.skip=true -DBUILD_NUMBER=engine` → `dist/target/Citizens-2.0.43-bengine.jar`.
> Aşağıdaki "yerel pom değişikliği" bölümü `master` için geçerli tarihçedir.

## Hızlı komut

```bash
mvn -B clean package -DskipTests -Dmaven.javadoc.skip=true -DBUILD_NUMBER=local
```

Çıktı: `dist/target/Citizens-2.0.43-blocal.jar`
Sunucuya kopyala: `plugins/` klasörü. Paper/Spigot 26.1.x ile çalışır.

## Ortam

| Gereksinim | Bu makinedeki durum |
|---|---|
| JDK | GraalVM 25 (`C:\Program Files (x86)\Java\graalvm-jdk-25.0.3+9.1`) — BuildTools `javaVersions: [69, 70]` kabul ediyor. GraalVM 21 de var. |
| Maven | 3.9.16 (`scoop`) |
| git | 2.52.0 — `core.autocrlf` **set edilmemiş** (BuildTools bunu ister, `true` olursa patch aşaması patlar) |
| Disk | C: dar (~3 GB boş). Her BuildTools koşusu ~2 GB ister. |

## EN ÖNEMLİ KURAL: NMS modülleri gerçek Spigot jar'ı ister

`main` modülü sadece Bukkit API kullandığı için Paper jar'ıyla da derlenir. **`v26_1_R1` gibi NMS modülleri derlenmez.**

Paper jar'ı `org.spigotmc:spigot` koordinatına kurulursa şu hatalar gelir:

```
attempting to assign weaker access privileges; was public     (~190 adet)
cannot find symbol: class CraftSplashPotion
cannot find symbol: class CraftLingeringPotion
cannot find symbol: method detectEquipmentUpdatesPublic()
```

Sebep: Paper'ın mojang-mapped jar'ı access widening uygular ve Spigot'a özel `Craft*` sınıfları/metodları yoktur. Çözüm Paper'a göre yamalamak **değil**, BuildTools ile gerçek Spigot jar üretmektir. (`dist` manifest'i zaten `paperweight-mappings-namespace: spigot` yazıyor → jar spigot mapping'e göre derlenir, Paper runtime'da remap eder.)

Kontrol: jar gerçek Spigot mi?

```bash
unzip -p ~/.m2/repository/org/spigotmc/spigot/26.1.2-R0.1-SNAPSHOT/spigot-26.1.2-R0.1-SNAPSHOT.jar META-INF/MANIFEST.MF | head
```

`Implementation-Title: Paper` veya `Main-Class: io.papermc.paperclip.Main` görürsen → YANLIŞ jar. BuildTools çıktısı ~25 MB'tır; Paper server jar'ı 80-175 MB.

## BuildTools ile Spigot jar üretme

```bash
mkdir -p /c/tmp/bt && cd /c/tmp/bt
curl -sSL -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
java -jar BuildTools.jar --rev 26.1.2
```

- Süre: ~10 dk. `~/.m2/repository/org/spigotmc/spigot/26.1.2-R0.1-SNAPSHOT/` içine kurar.
- 1.21.x modülleri (`v1_21_R7`) `remapped-mojang` classifier ister → o sürümler için `--remapped` bayrağı şart:
  `java -jar BuildTools.jar --rev 1.21.11 --remapped`
- Her MC sürümü için ayrı koşu gerekir (26.2 istersen `--rev 26.2`).

## Modül ↔ MC sürümü eşlemesi

`main/src/main/java/net/citizensnpcs/util/NMS.java:784` içindeki seçim:

| MC sürümü | Yüklenen sınıf |
|---|---|
| 26.1.x | `net.citizensnpcs.nms.v26_1_R1.util.NMSImpl` |
| 26.2.x | `v26_2_R1` |
| 1.21.11+ | `v1_21_R7` |

Jar içinde ilgili modül yoksa sunucu o sürümde `ClassNotFoundException` ile açılmaz.

## Bu repodaki yerel pom değişikliği (ÖNEMLİ)

Sadece 26.1.2 hedeflendiği ve diğer sürümlerin Spigot jar'ları m2'de olmadığı için **`v1_21_R7` ve `v26_2_R1` modülleri build'den çıkarıldı**:

- `pom.xml` → `spigot-release` profilindeki `<modules>` listesinden
- `dist/pom.xml` → `spigot-release` profilindeki `<dependencies>` listesinden

Sonuç: üretilen jar **sadece MC 26.1.x** destekler.

Ayrıca **`main/pom.xml` → `craftbukkit.version` = `26.1.2-R0.1-SNAPSHOT`** yapıldı (upstream'de 26.2). Böylece tüm repo tek bir Spigot artifact'ı ile derleniyor, m2'de başka sürüm tutmaya gerek yok.

Bunun gereği olarak **`main/.../trait/versioned/SulfurCubeTrait.java` silindi**: `EntityType.SULFUR_CUBE` sadece 26.2 API'sinde var. Dosya zaten işlevsiz bir stub'dı (`run()` boş, komut boş) ve başka hiçbir yerden referans verilmiyordu (versioned trait'ler otomatik keşfediliyor).

Upstream haline döndürmek:

```bash
git checkout pom.xml dist/pom.xml main/pom.xml
git checkout main/src/main/java/net/citizensnpcs/trait/versioned/SulfurCubeTrait.java
```

Ardından build almak için önce BuildTools ile `26.2` ve `1.21.11 --remapped` jar'ları kurulmalı.

## Diğer yerel yamalar (upstream'de yok)

| Dosya | Değişiklik |
|---|---|
| `Citizens.java` | PacketEvents API'si başka plugin tarafından kurulduysa üzerine yazılmıyor ve kapanışta `terminate()` edilmiyor |
| `PacketEventsHook.java` | Hologram metadata paketleri için per-packet dünya+registry lookup yerine spawn/despawn ile beslenen map; `disguiseTraitsUUID` remove bug'ı |
| `NMSBridge.java`, `NMS.java`, `NMSImpl.java`, `PacketNPC.java` | Packet NPC mount fix: link sonrası ve vehicle değişiminde `ClientboundSetPassengersPacket` yeniden gönderiliyor (aşağıya bak) |
| `HologramTrait.java` (`TextDisplayVehicleRenderer.render0`) | Mount yüksekliği passenger attachment noktasına göre düzeltildi (NPC içine gömülmüyor, scale attribute ile ölçekleniyor) + billboard her render'da yeniden uygulanıyor (varsayılan `CENTER`) |

### Packet hologram mount / offset sorunu

`npc.use-packet-holograms: true` iken hologram entity'si dünyaya eklenmiyor, sadece paketle gösteriliyor. Vehicle renderer'lar (`display_vehicle`, `armorstand_vehicle`, `interaction`) `NMS.mount()` çağırıyor ama `SetPassengers` paketi vehicle'ın (gerçek NPC) kendi tracker'ından, sahte entity client'ta daha var olmadan gidiyor → client mount'u yok sayıyor, hologram bağımsız hareket ediyor ve NPC yürürken kayıyor.

Yama: `PacketNPC.run()` içinde oyuncu link edildikten sonra ve vehicle her değiştiğinde `NMS.sendPassengers(player, vehicle)` çağrılıyor.

> **Sonuç (2026-08-01 testleri): `use-packet-holograms: false` kullan.** Teşhis logları `HologramTrait.run()`'ın çalıştığını, `defaultRenderer=TextDisplayVehicleRenderer` ve `packetHolograms=true` olduğunu doğruladı; buna rağmen client'ta ne mount ne billboard ne de yükseklik uygulanıyor. Vehicle renderer hologramı hiç teleport etmez, tamamen `NMS.mount()`'a güvenir — packet hologram'da entity dünyada olmadığı için `startRiding` tutmuyor ve hologram spawn noktasında kalıyor. Aynı config packet kapalıyken doğru çalışıyor. Packet hologram'ın kazancı da mütevazı: TextDisplay zaten AI'sız/fiziksiz ucuz bir entity, packet NPC ise `PlayerUpdateTask`'te her tick işleniyor ve kendi `ServerEntity.sendChanges`'ini çalıştırıyor.

Aynı sınıftan ikinci sorun: packet hologram'da display'in görünüm ayarları (billboard, transformation) entity metadata'sı olarak taşınıyor ve oyuncu tracker'a link olmadan önce set ediliyor; client bunları almadığı için vanilla varsayılanlarında kalıyor (`FIXED` billboard, translation yok → yazı kafa hizasında). Doğrulama: `use-packet-holograms: false` yapıldığında aynı config ile doğru çalışıyor. Yama: link anında `NMS.resendMetadata(player, entity)` ile entity'nin tüm non-default metadata'sı yeniden gönderiliyor (`ClientboundSetEntityDataPacket`).

Kullanım için config'de renderer vehicle'lı olmalı:

```yaml
npc:
  hologram:
    default-renderer: display_vehicle   # veya armorstand_vehicle
```

`default-renderer: display` mount yapmaz — hologram teleportla takip eder ve NPC'nin client tarafındaki interpolasyonu yüzünden yürürken önde görünür.

### display_vehicle yükseklik ve billboard

Upstream'de `TextDisplayVehicleRenderer.render0` translation'ı `offset.y + 0.2` yapıyordu. Mount edilen entity NPC'nin passenger attachment noktasında (model içinde, oyuncu için ~zeminden 1.35 blok) duruyor, dolayısıyla yazı NPC'nin gövdesiyle çakışıyordu. Yama:

```java
float aboveRider = NMS.getBoundingBoxHeight(npc.getEntity())
        - NMS.getRidingHeightOffset(npc.getEntity(), hologram.getEntity());
tf.getTranslation().y = offset.y + 0.2f + Math.max(0, aboveRider);
```

Böylece yazı `display` renderer ile aynı mutlak yükseklikte duruyor. Hem bounding box hem attachment noktası scale attribute ile ölçeklendiği için büyütülmüş NPC'lerde de doğru çalışıyor. Daha alçak/yüksek isterse `/npc hologram marginbottom` veya `npc.hologram.default-line-height` ile ayarlanır.

Billboard ayrı bir bug: `DisplayTrait.onSpawn()` billboard'ı `npc.getCosmeticEntity()` üzerinden uyguluyor, packet hologram bu yoldan geçmiyor → display vanilla varsayılanı `FIXED` kalıyor (config'de `billboard: CENTER` yazsa bile). Yama: `TextDisplayRenderer.applyBillboard()` eklendi, hem `display` hem `display_vehicle` render0'ından çağrılıyor; `dt`'de değer yoksa `CENTER` uygulanıyor.

## Sorun giderme

| Hata | Sebep / çözüm |
|---|---|
| `spigot:jar:remapped-mojang:... was not found` | O sürüm için `BuildTools --rev X --remapped` koşulmamış. |
| `attempting to assign weaker access privileges` | m2'deki "spigot" aslında Paper jar. BuildTools ile gerçeğini kur. |
| `cannot find symbol: CraftSplashPotion` | Aynı sebep (Paper jar). |
| `resolution is not reattempted until the update interval ... has elapsed` | Maven negatif cache. `-U` ekle veya m2'deki `*.lastUpdated` dosyalarını sil. |
| `dependencies.dependency.systemPath ... Vault.jar` (WARNING) | Zararsız, upstream uyarısı. Build'i etkilemez. |
| BuildTools patch hatası | `git config --global core.autocrlf false` olmalı. |

## Jar doğrulama

```bash
J=dist/target/Citizens-2.0.43-blocal.jar
unzip -l "$J" | grep -o "nms/v[0-9_A-Z]*" | sort -u      # hangi NMS modülleri var
unzip -p "$J" plugin.yml | head -6                        # sürüm / main class
unzip -p "$J" META-INF/MANIFEST.MF | grep paperweight     # spigot mapping namespace
```

Son doğrulanan build: `Citizens-2.0.43-blocal.jar` (2.55 MB), 469 adet `v26_1_R1` sınıfı, `paperweight-mappings-namespace: spigot`.
