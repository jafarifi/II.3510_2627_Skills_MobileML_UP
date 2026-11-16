# Guide de l’exercice — Mobile ML dans SwapCampus

## Le but

Vous allez compléter deux fonctionnalités présentées en classe : proposer une catégorie depuis une photo et lire un code-barres depuis une photo. L’interface et les appels à ML Kit sont déjà préparés. Il reste quatre petits traitements à écrire, signalés par `TODO 1` à `TODO 4`.

ML Kit est la bibliothèque de Google qui analyse les images. Vous n’avez pas à entraîner un modèle ni à créer un écran.

## 1. Ouvrir le projet

1. Décompressez l’archive puis ouvrez le dossier contenant `settings.gradle.kts` dans Android Studio.
2. Attendez la synchronisation Gradle. Le SDK de compilation demandé est 37 ; le téléphone ou l’émulateur peut utiliser une autre version Android (API 24 minimum).
3. Sélectionnez le module `app` et votre émulateur, puis lancez l’application.
4. Connectez-vous avec `alice@isep.fr` et `alice123`. Allez dans **My Listings**, puis **Post a listing** pour ouvrir l’écran de création d’annonce.

Avant les modifications, l’application démarre mais les deux fonctions ML ne donnent pas de résultat utile : c’est le point de départ de l’exercice.

## 2. Trouver les quatre endroits

Dans Android Studio, utilisez **Edit → Find → Find in Files**, puis cherchez `TODO 1`, et ensuite les suivants.

Modifiez seulement les zones indiquées. Conservez les noms des fonctions, leurs paramètres, les imports et les autres appels déjà présents. Quand une consigne demande de renvoyer un résultat, remplacez le résultat provisoire : ne laissez pas deux appels à `onResult`.

Quelques mots utiles :

- Une **étiquette** est un mot reconnu dans une image, comme `Paper` ou `Laptop`.
- Le **score de confiance** est un nombre entre 0 et 1 : 0,81 correspond à 81 %.
- `shared` contient ici les traitements Kotlin sans dépendance à Android.
- Un **callback** est une fonction appelée quand le traitement se termine. Ici, `onResult` transmet un résultat et `onError` transmet une erreur.
- `null` signifie qu’une valeur est absente ; une chaîne vide ou composée d’espaces ne contient pas de texte utile.

Aide pour les exos : 

## TODO 1 — Choisir la catégorie

**Fichier :** `shared/src/commonMain/kotlin/com/isep/shared/domain/CategorySuggester.kt`

**Où :** dans la fonction `suggest`. Le résultat provisoire est `Category.OTHER`.

**Entrée :** une liste de `DetectedLabel`. Chaque élément contient `text` (le mot) et `confidence` (son score).

**Sortie :** une seule `Category`.

À faire dans cet ordre :

1. Écarter les éléments dont le score est inférieur à `MIN_CONFIDENCE` (0,7).
2. Classer les autres du score le plus élevé au plus faible.
3. Chercher chaque mot dans la table `labelToCategory` déjà fournie, en le mettant en minuscules.
4. Renvoyer la première catégorie trouvée. Si aucun mot ne correspond, renvoyer `Category.OTHER`.

**Exemple :** `Paper` à 0,81 et `Laptop` à 0,60 doivent donner `BOOKS`. Une liste vide doit donner `OTHER`. Un mot inconnu, même très fiable, ne doit pas empêcher de chercher une correspondance dans les suivants.

**Indices Kotlin :** `filter` garde les éléments qui respectent une condition ; `sortedByDescending` trie du plus grand au plus petit ; `lowercase()` met un texte en minuscules. Une boucle `for` peut parcourir les éléments. L’accès `labelToCategory[mot]` renvoie une catégorie ou `null`.

## TODO 2 — Transmettre les étiquettes à l’application

**Fichier :** `app/src/main/java/com/isep/swapcampus/ml/ImageLabelerHelper.kt`

**Où :** dans `addOnSuccessListener`, à la place de `onResult(emptyList())`.

**Entrée :** `labels`, la liste des résultats de Google. Chaque résultat possède `text` et `confidence`.

**Résultat à transmettre :** une liste de nos objets `DetectedLabel`, avec les mêmes mots et les mêmes scores.

1. Transformer chaque élément Google en `DetectedLabel`.
2. Passer la nouvelle liste à `onResult` une seule fois.

**Exemple :** une étiquette Google `Paper`, score 0,81, devient `DetectedLabel("Paper", 0.81f)`.

**Indice Kotlin :** `map` transforme chaque élément d’une liste pour créer une nouvelle liste. La classe `DetectedLabel` est déjà importée. Ne changez pas la gestion des erreurs ni la fermeture du lecteur.

## TODO 3 — Récupérer les valeurs des codes-barres

**Fichier :** `app/src/main/java/com/isep/swapcampus/ml/BarcodeHelper.kt`

**Où :** dans `addOnSuccessListener`, à la place de `onResult(emptyList())`.

**Entrée :** `barcodes`, la liste des codes détectés par ML Kit. La propriété `rawValue` contient le texte lu et peut être `null`.

**Résultat à transmettre :** une liste de textes non vides, sans doublon.

1. Récupérer `rawValue` pour chaque code.
2. Ignorer les valeurs absentes et les textes vides ou composés d’espaces.
3. Retirer les valeurs identiques.
4. Appeler `onResult` avec la liste obtenue, même si elle est vide.

**Exemple :** les valeurs `9780134685991`, `null`, trois espaces et `9780134685991` doivent donner une liste contenant une seule fois `9780134685991`.

**Indices Kotlin :** `mapNotNull` permet d’ignorer les résultats absents ; `?.` accède à une propriété ou une fonction uniquement si la valeur existe ; `isNotBlank()` vérifie qu’un texte est utile ; `takeIf` garde une valeur si une condition est vraie ; `distinct()` retire les doublons. Vous pouvez aussi construire une liste avec une boucle.

## TODO 4 — Ajouter la référence à la description

**Fichier :** `shared/src/commonMain/kotlin/com/isep/shared/domain/BarcodeText.kt`

**Où :** dans la fonction `appendToDescription`, à la place du simple `return description`.

**Entrées :** `description`, le texte déjà écrit, et `value`, le code choisi.

**Sortie :** la description avec la référence ajoutée si nécessaire.

1. Retirer les espaces autour du code avec `trim()`.
2. Si le code est vide, conserver la description.
3. Préparer une ligne sous la forme `Barcode: valeur`.
4. Si cette ligne existe déjà dans la description, conserver la description.
5. Sinon, ajouter la ligne après le texte, avec un retour à la ligne. Si la description est vide ou composée d’espaces, renvoyer seulement la nouvelle ligne.

**Exemples :**

- Description `Livre en bon état`, code `123` → deux lignes : `Livre en bon état`, puis `Barcode: 123`.
- Description vide, code `123` → `Barcode: 123`.
- Code composé d’espaces → description inchangée.
- Description contenant déjà la ligne `Barcode: 123`, code `123` → description inchangée.

**Indices Kotlin :** `lineSequence()` parcourt les lignes d’un texte ; `any` vérifie si au moins une ligne respecte une condition ; `isBlank()` détecte un texte vide ou composé d’espaces ; `\n` représente un retour à la ligne ; `trimEnd()` retire les espaces et retours à la ligne à la fin du texte.

## 3. Vérifier votre travail

Enregistrez les fichiers. Dans le Terminal d’Android Studio, à la racine du projet, lancez :

```sh
./gradlew :shared:testAndroidHostTest
```

Ces tests vérifient les règles Kotlin, notamment les TODO 1 et 4. Un test en échec indique le cas à revoir. `BUILD SUCCESSFUL` indique que la commande a réussi.

Allumez ensuite un émulateur Android, puis lancez :

```sh
./gradlew :app:connectedDebugAndroidTest
```

Cette commande vérifie notamment la lecture réelle d’un code-barres et d’une image sans code. Les images utilisées par ces tests se trouvent dans `app/src/androidTest/assets`.

## 4. Essayer dans l’application

Les images de démonstration sont dans `demo/`.

1. Glissez `ISBN_demo.png` et `Sans_code.png` dans l’émulateur, puis retrouvez-les dans le sélecteur de photos. Si nécessaire, ouvrez la collection Téléchargements ou l’application Photos pour les retrouver.
2. Dans une annonce, écrivez une courte description.
3. Appuyez sur **Scan barcode from photo** et choisissez `ISBN_demo.png`.
4. Vérifiez que **Barcode / ISBN** affiche `9780134685991`.
5. Appuyez deux fois sur **Add to description** : la référence doit apparaître une seule fois, et votre texte doit rester présent.
6. Enregistrez l’annonce avec **Save**, puis rouvrez-la pour vérifier la description.
7. Recommencez le scan avec `Sans_code.png` : un message doit signaler qu’aucun code lisible n’a été trouvé.
8. Pour la catégorie, choisissez une photo nette d’un objet reconnu, comme un livre ou un ordinateur, avec **Suggest category from photo**. La catégorie doit être proposée ; elle reste modifiable par l’utilisateur. Le résultat dépend de la photo.

La lecture du code renvoie sa valeur : elle ne recherche pas le titre, l’auteur ou le prix d’un livre sur Internet. L’analyse utilise une photo choisie dans la galerie, pas une caméra en continu.

## Si vous êtes bloqués

Commencez par les exemples du TODO concerné. Vérifiez le type attendu en sortie : une catégorie, une liste ou un texte. Faites un seul TODO à la fois, puis relancez les tests correspondants. Les indices ci-dessus donnent les opérations utiles sans fournir la fonction complète.
