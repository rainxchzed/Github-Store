<div align="center">
<img src="https://github.com/rainxchzed/Github-Store/blob/main/composeApp/src/commonMain/composeResources/drawable/app-icon.png" width="200" alt="Project logo"/>
</div>

<h1 align="center">GitHub Store</h1>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://kotlinlang.org"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF.svg"/></a>
  <a href="#"><img alt="Platforms" src="https://img.shields.io/badge/Platforms-Android%20%7C%20Desktop-brightgreen"/></a>
  <a href="https://github.com/rainxchzed/Github-Store/releases">
    <img alt="Release" src="https://img.shields.io/github/v/release/rainxchzed/Github-Store?label=Release&logo=github"/>
  </a>
  <a href="https://github.com/rainxchzed/Github-Store/stargazers">
    <img alt="GitHub stars" src="https://img.shields.io/github/stars/rainxchzed/Github-Store?style=social"/>
  </a>
  <img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white"/>
  <img alt="Koin" src="https://img.shields.io/badge/DI-Koin-3C5A99?logo=kotlin&logoColor=white"/>
</p>

<p align="center">
<a href="/docs/README-RU.md" target="_blank"> Русский </a> | <a href="/README.md" target="_blank"> English </a> | <a href="/docs/README-ES.md" target="_blank"> Español </a> | <a href="/docs/README-FR.md" target="_blank"> Français </a> | <a href="/docs/README-KR.md" target="_blank"> 한국어 </a> | <a href="/docs/README-ZH.md" target="_blank">中文</a> | <a href="/docs/README-JA.md" target="_blank">日本語</a> | <a href="/docs/README-PL.md" target="_blank">Polski</a>
</p>

<p align="center">
GitHub Store to wieloplatformowy „play store” dla realasów repozytoriów GitHub. 
  Prezentuje repozytoria, które udostępniają rzeczywiste, instalowalne pliki binarne, i umożliwia ich instalowanie, śledzenie oraz aktualizowanie na różnych platformach – wszystko z jednego miejsca.
</p>

<p align="center">
  <img src="../screenshots/banner.png" />
</p>

---

### Zrzuty ekranu znajdziesz w folderze [screenshots/](screenshots/).

<img src="/screenshots/preview.gif" align="right" width="320"/>

## ✨ Czym jest GitHub Store?

GitHub Store to aplikacja Kotlin Multiplatform (Android + Desktop), która przekształca wydania GitHub w
przejrzystą aplikację w stylu sklepu z aplikacjami:

- Wyświetla tylko repozytoria, które faktycznie zawierają pliki instalacyjne (APK, EXE, DMG, AppImage, DEB, RPM itp.).
- Wykrywa platformę użytkownika i wyświetla odpowiedni instalator.
- Zawsze instaluje najnowszą opublikowaną wersję, podświetla listę zmian i może powiadamiać o dostępności aktualizacji zainstalowanych aplikacji (w systemie Android).
- Wyświetla przejrzysty ekran szczegółów zawierający statystyki, plik README i informacje o deweloperze.

---

[FAQ](https://github.com/rainxchzed/Github-Store/wiki/FAQ)

---

## 🔃 Download

<a href="https://github.com/rainxchzed/Github-Store/releases">
   <image src="https://i.ibb.co/q0mdc4Z/get-it-on-github.png" height="80"/>
 </a>

<a href="https://f-droid.org/en/packages/zed.rainxch.githubstore/">
   <image src="https://f-droid.org/badge/get-it-on.png" height="80"/>
   </a>
   
> [!IMPORTANT]
> W systemie macOS może pojawić się ostrzeżenie, że firma Apple nie może zweryfikować, czy sklep GitHub Store jest wolny od złośliwego oprogramowania. Dzieje się tak,
> ponieważ aplikacja jest dystrybuowana poza sklepem App Store i nie została jeszcze poświadczona notarialnie. Można ją zezwolić
> poprzez Ustawienia systemowe → Prywatność i bezpieczeństwo → Otwórz mimo to.

## 🏆 Featured In

<a href="https://www.youtube.com/@howtomen">
  <img src="https://img.shields.io/badge/Featured%20by-HowToMen-red?logo=youtube" alt="Featured by HowToMen">
</a>

- **HowToMen**: [Top 20 Best Android Apps 2026 (860K subscribers)](https://www.youtube.com/watch?v=7favc9MDedQ)
- **F-Droid**: [#1 in App Store category](https://f-droid.org/en/categories/app-store-updater/)

## 🚀 Funkcjonalności

- **Inteligentne wyszukiwanie**
- Sekcje główne dla projektów „Trendy”, „Ostatnio zaktualizowane” i „Nowe” z filtrami czasowymi.
- Wyświetlane są tylko repozytoria z ważnymi zasobami, które można zainstalować.
- Ocena tematów uwzględniająca platformę, dzięki czemu użytkownicy Androida/komputerów stacjonarnych najpierw widzą odpowiednie aplikacje.

- **Instalacje najnowszych wersji**
- Pobiera `/releases/latest` dla każdego repozytorium.
- Wyświetla tylko zasoby z najnowszej wersji.
- Pojedyncza akcja „Zainstaluj najnowszą wersję” oraz rozwijana lista wszystkich instalatorów dla tej wersji.

- **Ekran z bogatymi szczegółami**
- Nazwa aplikacji, wersja, przycisk „Zainstaluj najnowszą wersję”.
- Gwiazdki, rozwidlenia, otwarte zgłoszenia.
- Wyświetlana treść pliku README („O tej aplikacji”).
- Najnowsze informacje o wydaniu (treść) z formatowaniem markdown.
- Lista instalatorów z oznaczeniami platform i rozmiarami plików.

- **Wieloplatformowy interfejs użytkownika**
- Android: otwiera pliki APK do pobrania za pomocą instalatora pakietów, śledzi instalacje w lokalnej bazie danych i wyświetla je na dedykowanym ekranie aplikacji wraz ze wskaźnikami aktualizacji.
- Komputery stacjonarne (Windows/macOS/Linux): pobiera instalatory do folderu „Pobrane” użytkownika i otwiera je za pomocą domyślnego programu obsługującego; brak ukrytych lokalizacji tymczasowych.

- **Wygląd i motywy**
- Projekt Material 3 z komponentami **Material 3 Expressive** na wszystkich platformach.
- Obsługa dynamicznych kolorów Material You w systemie Android, jeśli jest dostępna.
- Opcjonalny tryb AMOLED black dla ciemnego motywu na urządzeniach OLED.

- **Bezpieczeństwo i kontrola (Android)**
- Opcjonalne logowanie do GitHub za pośrednictwem przepływu urządzeń OAuth w celu uzyskania wyższych limitów szybkości przy minimalnym zakresie.
- Akcja „Otwórz w AppManager” umożliwiająca sprawdzenie uprawnień i trackerów pliku APK przed instalacją.


---

## 🔍 W jaki sposób moja aplikacja pojawia się w sklepie GitHub Store?

Sklep GitHub nie stosuje żadnych prywatnych zasad indeksowania ani ręcznej selekcji.  
Twój projekt może pojawić się automatycznie, jeśli spełnia następujące warunki:

1. **Publiczne repozytorium na GitHub**
- Widoczność musi być ustawiona jako „publiczna”.

2. **Co najmniej jedna opublikowana wersja**
- Utworzona za pomocą GitHub Releases (nie tylko tagów).
- Najnowsza wersja nie może być wersją roboczą ani przedpremierową.

3. **Zasoby, które można zainstalować w najnowszej wersji**
- Najnowsza wersja musi zawierać co najmniej jeden plik zasobów z obsługiwanym rozszerzeniem:
- Android: `.apk`
- Windows: `.exe`, `.msi`
- macOS: `.dmg`, `.pkg`
- Linux: `.deb`, `.rpm`, `.AppImage`
    - Sklep GitHub ignoruje automatycznie generowane artefakty źródłowe GitHub (`Kod źródłowy (zip)` /
      `Kod źródłowy (tar.gz)`).

4. **Wyszukiwalne według wyszukiwania / tematów**
- Repozytoria są pobierane za pośrednictwem publicznego interfejsu API wyszukiwania GitHub.
    - Temat, język i opis pomagają w rankingu:
- Aplikacje na Androida: tematy takie jak `android`, `mobile`, `apk`.
- Aplikacje desktopowe: tematy takie jak `desktop`, `windows`, `linux`, `macos`, `compose-desktop`,
          `electron`.
    - Posiadanie co najmniej kilku gwiazdek zwiększa prawdopodobieństwo pojawienia się w sekcjach Popularne/Zaktualizowane/Nowe.

Jeśli Twoje repozytorium spełnia te warunki, GitHub Store może je znaleźć za pomocą wyszukiwania i wyświetlić
automatycznie — bez konieczności ręcznego przesyłania.

---

## 🧭 Jak działa GitHub Store (ogólny opis)

1. **Wyszukiwanie**
  - Wykorzystuje punkt końcowy GitHub `/search/repositories` z zapytaniami dostosowanymi do platformy.
  - Stosuje prostą punktację opartą na tematach, języku i opisie.
  - Filtruje zarchiwizowane repozytoria i te z zbyt małą liczbą sygnałów.

2. **Sprawdzenie wydania + zasobów**
  - W przypadku repozytoriów kandydujących wywołuje `/repos/{owner}/{repo}/releases/latest`.
  - Sprawdza tablicę `assets` pod kątem rozszerzeń plików specyficznych dla platformy.
  - Jeśli nie znaleziono odpowiednich zasobów, repozytorium jest wykluczane z wyników.

3. **Ekran szczegółów**
- Informacje o repozytorium: nazwa, właściciel, opis, gwiazdki, rozwidlenia, problemy.
- Najnowsza wersja: tag, data publikacji, treść (lista zmian), zasoby.
- README: ładowane z domyślnej gałęzi i renderowane jako „O tej aplikacji”.

4. **Przebieg instalacji**
    - Gdy użytkownik kliknie „Zainstaluj najnowszą wersję”:
- Wybiera zasób najlepiej pasujący do bieżącej platformy.
- Rozpoczyna pobieranie.
- Przekazuje do instalatora systemu operacyjnego (instalator APK w systemie Android, domyślny program obsługi na komputerze stacjonarnym).
- W systemie Android rejestruje instalację w lokalnej bazie danych i korzysta z monitorowania pakietów, aby zsynchronizować listę zainstalowanych elementów.

---

## ⚙️ Tech stack

- **Minimum Android SDK: 24**

- **Język i platforma**
    - [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) (Android + JVM Desktop)
    - [Compose Multiplatform UI](https://www.jetbrains.com/compose-multiplatform/) ([Material 3](https://m3.material.io/),
      icons, resources)

- **Async & state**
    - [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) + [Flow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/)
    - AndroidX Lifecycle (ViewModel + Runtime Compose)

- **Networking & Data**
    - [Ktor 3](https://ktor.io/) (HttpClient with OkHttp on Android, Java on Desktop)
    - [Kotlinx Serialization JSON](https://github.com/Kotlin/kotlinx.serialization).
    - [Kotlinx Datetime](https://github.com/Kotlin/kotlinx-datetime) for time handling
    - [Room](https://developer.android.com/jetpack/androidx/releases/room) + KSP for the installed apps database on Android.

- **Dependency injection**
    - [Koin 4](https://insert-koin.io/)

- **Navigation**
    - [JetBrains Navigation Compose](https://kotlinlang.org/docs/multiplatform/compose-navigation.html)
      for shared navigation graph

- **Auth & Security**
    - GitHub OAuth (Device Code flow)
    - [Androidx DataStore](https://developer.android.com/kotlin/multiplatform/datastore) for token
      storage

- **Media & markdown**
    - [Coil 3](https://coil-kt.github.io/coil/getting_started/) (Ktor3 image loader)
    - [multiplatform-markdown-renderer-m3](https://github.com/mikepenz/multiplatform-markdown-renderer) (+
      Coil3 integration) for README/release notes

- **Logging & tooling**
    - [Kermit logging](https://kermit.touchlab.co/)
    - [Compose Hot Reload](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html) (
      desktop)
    - [ProGuard/R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization) +
      resource shrinking for release builds

---

## ✅ Zalety / Dlaczego warto korzystać z GitHub Store?

- **Koniec z przeszukiwaniem wydań GitHub**
  Wyświetlane są tylko repozytoria, które faktycznie zawierają pliki binarne dla Twojej platformy.

- **Wiedza o zainstalowanych aplikacjach**
  Śledzi aplikacje zainstalowane za pośrednictwem GitHub Store (Android) i zaznacza dostępność nowych wydań, dzięki czemu można je aktualizować bez konieczności ponownego przeszukiwania GitHub.

- **Zawsze najnowsza wersja**  
  Instalacje pochodzą z najnowszej opublikowanej wersji; widoczny dziennik zmian
  odzwierciedla dokładnie to, co instalujesz.

- **Jednolite doświadczenie na wszystkich platformach**  
  Ten sam interfejs użytkownika i logika dla Androida i komputerów stacjonarnych, z natywnym dla platformy zachowaniem instalacyjnym.

- **Otwarta platforma i możliwość rozbudowy**  
  Napisany w KMP z wyraźnym rozdzieleniem sieci, logiki domeny i interfejsu użytkownika — łatwy do rozwidlenia,
  rozbudowy lub dostosowania.
  
---

## 💖 Wesprzyj ten projekt

Sklep GitHub jest bezpłatny i zawsze taki pozostanie. Jeśli okazał się dla Ciebie pomocny, rozważ:

<a href="https://github.com/sponsors/rainxchzed">
  <img src="https://img.shields.io/badge/Sponsor-GitHub-pink?logo=github" alt="Sponsor on GitHub">
</a>

<a href="https://www.buymeacoffee.com/rainxchzed">
  <img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-FFDD00?logo=buy-me-a-coffee&logoColor=black" alt="Buy Me a Coffee">
</a>

Twoje wsparcie pomaga mi:
- Utrzymywać aplikację dla ponad 20 tysięcy użytkowników
- Tworzyć nowe funkcje
- Tworzyć więcej bezpłatnych narzędzi dla programistów

Możesz też oznaczyć repozytorium gwiazdką i udostępnić je innym! ⭐

## Star History

<a href="https://www.star-history.com/#rainxchzed/Github-Store&type=timeline&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=rainxchzed/Github-Store&type=timeline&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=rainxchzed/Github-Store&type=timeline&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=rainxchzed/Github-Store&type=timeline&legend=top-left" />
 </picture>
</a>

## 🔑 Konfiguracja

Sklep GitHub Store wykorzystuje aplikację GitHub OAuth do uwierzytelniania i izolacji limitów API.

1. Utwórz aplikację GitHub OAuth w **GitHub → Ustawienia → Ustawienia programisty → Aplikacje OAuth**.
2. Ustaw adres URL wywołania zwrotnego na `githubstore://callback` (_Nie jest to zbyt ważne_).
3. Skopiuj **identyfikator klienta** z aplikacji OAuth.
4. W pliku `local.properties` swojego projektu dodaj:

---

## ⚠️ Disclaimer
Sklep GitHub Store pomaga jedynie w wyszukiwaniu i pobieraniu zasobów, które zostały już opublikowane w serwisie
GitHub przez zewnętrznych programistów.  
Za zawartość, bezpieczeństwo i działanie tych plików do pobrania odpowiedzialność ponoszą wyłącznie ich
autorzy i dystrybutorzy, a nie niniejszy projekt.

Korzystając z GithubStore, użytkownik rozumie i zgadza się, że instaluje i uruchamia wszelkie pobrane oprogramowanie na
własne ryzyko.  
Projekt ten nie weryfikuje, nie potwierdza ani nie gwarantuje, że jakikolwiek instalator jest bezpieczny, wolny od złośliwego oprogramowania lub
odpowiedni do określonego celu.

## Stats
![Alt](https://repobeats.axiom.co/api/embed/20367dca127572e9c47db33850979d78df2c6a8b.svg "Repobeats analytics image")

## 📄 License

GitHub Store will be released under the **Apache License, Version 2.0**.

```
Copyright 2025 rainxchzed

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
