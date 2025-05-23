# SSTRANGE: Scalable Similarity TRacker in Academia with Natural lanGuage Explanation
**SSTRANGE** \(Scalable Similarity TRacker in Academia with Natural lanGuage Explanation\) is a scalable and efficient tool to observe similarities among submissions with locality sensitive hashing: MinHash and Super-Bit. Currently, the tool supports Java, Python, C#, Dart, and Web (HTML+JS+CSS+PHP) submissions. It also incorporates sensitive similarity.

General details for the base version can be seen in [the corresponding paper](https://www.mdpi.com/2227-7102/13/1/54) published in MDPI's Education Sciences as part of special issue "Application of New Technologies for Assessment in Higher Education", invited by Mike Joy and Peter Williams. Details of C# mode can be seen in [this paper](https://ieeexplore.ieee.org/abstract/document/10260942) published in 2023 IEEE International Conference on Advanced Learning Technologies (ICALT) and it is the best discussion paper on that conference. Details of sensitive similarity can be seen in [this paper](https://ieeexplore.ieee.org/abstract/document/10500603/) published in 2024 IEEE World Engineering Education Conference (EDUNINE).



Unlike its counterpart, [Comprehensive STRANGE](https://github.com/oscarkarnalim/CSTRANGE), SSTRANGE focuses on efficiency and it is suitable for large submissions. SSTRANGE can also be executed via command line interface (see the instructions below). For comprehensive reporting, it is recommended to use [Comprehensive STRANGE or CSTRANGE](https://github.com/oscarkarnalim/CSTRANGE) instead.

SSTRANGE and CSTRANGE has comparable features: graphical user interface, template code removal, and common code removal. 

## User Interface
### Main layout
<p align="center">
<img width="40%" src="https://github.com/oscarkarnalim/SSTRANGE/blob/main/UI_01.png?raw=true">
</p>

### Navigation layout
<p align="center">
<img width="80%" src="https://github.com/oscarkarnalim/SSTRANGE/blob/main/UI_02.png?raw=true">
</p>

### Pairwise similarity report
<p align="center">
<img width="80%" src="https://github.com/oscarkarnalim/SSTRANGE/blob/main/UI_03.png?raw=true">
</p>

## Input parameters
### Assessment path
This refers to a directory containing student submissions as submission files, sub-directories or zip files.

### Submission type
Single file: each submission is represented with either a file or a sub-directory with one file.  
Multiple files in a directory: each submission is represented with a sub-directory containing multiple files. All files will be concatenated prior comparison.  
Multiple files in a zip: each submission is represented with a zip. The zip will be unzipped and all of its files will be concatenated prior comparison.  

### Submission language
The programming language of the submissions.

### Explanation language
The human language of similarity explanation. The options are English and Indonesian.

### Minimum similarity threshold
The minimum percentage of similarity for a submission pair to be reported. The value is from 0 to 100 inclusive. 

### Maximum reported submission pairs
The maximum number of reported submission pairs with high similarity. Larger value will display more submission pairs for manual check but will make the execution runs slower.

### Minimum matching length
It defines how many similar tokens ('words') are required for a part of the content to be reported. Larger value will mitigate the occurrence of coincidental similarity, but it will make the tool less resilient to disguises.

### Similarity measurement
How similarities will be detected. The options are MinHash, Super-Bit, Jaccard, Cosine, and RKRGST (running Karp-Rabin greedy string tiling). MinHash and Super-Bit are the most time efficient while RKRGST is the slowest.

### Dissimilarity threshold
How unique a submission so that it can be reported as ``overly unique''. The submission might be a result from another collague. The value is from 0 to 100 inclusive. 

### AI submission path
This refers to a directory containing content generated by AI as a sample. The tool will report any submissions that are similar to it as ``suspected of AI misuse''. 

## Command line usage
```
<assessment path> <submission type> <submission language> <explanation language> <minimum similarity threshold> <maximum reported submission pairs> <minimum matching length> <template directory path> <common content> <similarity measurement> <resource path> <dissimilarity threshold> <AI submission path> <number of clusters> <number of stages>


<assessment path>: absolute directory path (String) 
<submission type>: "file", "dir", or "zip" 
<submission language>: "java", "py", "web", "dart", or "csharp" 
<explanation language>: "en" or "id" 
<minimum similarity threshold>: non-negative integer 0-100 
<maximum reported submission pairs>: positive integer 
<minimum matching length>: positive integer no less than 2 
<similarity measurement>: "minhash", "super-bit", "jaccard", "cosine", "rkrgst", "sensitive minhash", "sensitive super-bit", "sensitive jaccard", "sensitive cosine", or "sensitive rkrgst"  
<resource path>: absolute directory path to SSTRANGE resource dir (String)
<dissimilarity threshold>: non-negative integer 0-100 
<AI submission path>: absolute directory path (String) 
<number of clusters>: positive integer no less than 2 (only used for minhash and super-bit) 
<number of stages>: positive integer (only used for minhash and super-bit) 
```

## Acknowledgments
This tool uses [STRANGE](https://github.com/oscarkarnalim/strange) as the basis of development, [CSTRANGE](https://github.com/oscarkarnalim/strange) to develop user interface, [ANTLR](https://www.antlr.org/) to tokenise given submissions, [tdebatty's module](https://github.com/tdebatty/java-LSH) to do locality sensitive hashing, and [Google Prettify](https://github.com/google/code-prettify) to display source code.

# Indonesian Guideline for SSTRANGE
**SSTRANGE** \(Scalable Similarity TRacker in Academia with Natural lanGuage Explanation\) adalah kakas yang skalabel dan efisien untuk mengobservasi kesamaan tugas program siswa dengan locality sensitive hashing: MinHash and Super-Bit. Saat ini, kakas hanya mendukung bahasa pemrograman Java dan Python. Detil lebih jauh dapat dilihat di [artikel terkait](https://www.mdpi.com/2227-7102/13/1/54) yang dipublikasikan di MDPI's Education Sciences sebagai bagian dari edisi khusus "Application of New Technologies for Assessment in Higher Education", diundang oleh Mike Joy dan Peter Williams. Detil dari mode C# dapat dilihat di [artikel ini](https://ieeexplore.ieee.org/abstract/document/10260942) yang dipublikasikan di 2023 IEEE International Conference on Advanced Learning Technologies (ICALT) dan merupakan paper diskusi terbaik pada konferensi tersebut.

Tidak seperti padanannya, [Comprehensive STRANGE](https://github.com/oscarkarnalim/CSTRANGE), SSTRANGE terfokus pada efisiensi dan cocok digunakan untuk tugas-tugas berukuran besar. SSTRANGE juga dapat dijalankan melalui command line interface (lihat instruksinya dibawah). Untuk pelaporan kesamaan yang kopmrehensif, anda direkomendasikan untuk menggunakan [Comprehensive STRANGE or CSTRANGE](https://github.com/oscarkarnalim/CSTRANGE).

SSTRANGE dan CSTRANGE memiliki fitur yang komparabel: tampilan antarmuka, penghapusan kode template, dan penghapusan kode umum. 

## Masukan 
### Assessment path (lokasi tugas)
Ini mengarah pada sebuah direktori berisi kumpulan hasil pekerjaan dalam bentuk kumpulan file, sub-direktori, atau zip.

### Submission type (jenis hasil pekerjaan yang dikumpulkan)
Single file: setiap tugas siswa direpresentasikan dengan sebuah file atau sebuah sub-direktori berisi satu file hasil pekerjaan.  
Multiple files in a directory: setiap tugas siswa direpresentasikan dengan sub-direktori berisi beberapa file. Semua file tersebut akan dikonkatenasi sebelum dibandingkan satu sama lain.  
Multiple files in a zip: setiap tugas siswa direpresentasikan dengan sebuah file zip. File tersebut akan diekstrak dan semua file didalamnya akan dikonkatenasi sebelum dibandingkan satu sama lain.

### Submission language (bahasa hasil pekerjaan)
Bahasa pemrograman dari hasil pekerjaan yang dikumpulkan. 

### Explanation language (bahasa penjelasan)
Bahasa manusia dari penjelasan kesamaan. Pilihan yang ada adalah Inggris dan Indonesia.  

### Minimum similarity threshold (batas kesamaan minimum)
Persentasi minimum dari kesamaan hasil pekerjaan agar dilaporkan. Nilainya diantara 0 hingga 100 secara inklusif. 

### Maximum reported submission pairs (batas maksimum pasangan tugas yang dilaporkan)
Jumlah maksimum dari pasangan hasil pekerjaan dengan kesamaan tinggi yang dilaporkan. Nilai besar akan menampilkan lebih banyak pasangan tugas untuk cek manual tapi akan membuat eksekusi menjadi lebih lambat.

### Minimum matching length (panjang kesamaan minimum)
Ini menentukan seberapa banyak token ('kata') yang dibutuhkan agar sebuah bagian konten dilaporkan. Nilai yang lebih besar akan mengurangi kemunculan kesamaan tidak disengaja, namun akan membuat kakas semakin rentan dengan penyamaran.

### Similarity measurement (pengukuran kesamaan)
Bagaimana kesamaan kode dapat dideteksi. Pilihannya antara lain MinHash, Super-Bit, Jaccard, Cosine, dan RKRGST (running Karp-Rabin greedy string tiling). MinHash dan Super-Bit merupakan dua algoritma yang paling efisien sedangkan RKRGST adalah yang terlambat.

### Dissimilarity threshold (batas minimum keunikan)
Seberapa unik sebuah hasil pekerjaan hingga bisa dikategorikan sebagai ``terlalu unik''. Hasil pekerjaan tersebut bisa jadi hasil karya orang lain. Nilainya diantara 0 hingga 100 secara inklusif. 

### AI submission path (lokasi direktori hasil pekerjaan AI)
Ini mengarah pada lokasi direktori berisi konten kode yang dibangkitkan oleh kecerdasan buatan (AI). Kakas akan melaporkan semua hasil karya yang sama dengannya sebagai ``dicurigai melibatkan penyalahgunaan AI''. 

## Penggunaan dengan command line
```
<assessment path> <submission type> <submission language> <explanation language> <minimum similarity threshold> <maximum reported submission pairs> <minimum matching length> <template directory path> <common content> <similarity measurement> <resource path> <dissimilarity threshold> <AI submission path> <number of clusters> <number of stages>


<assessment path>: path direktori absolut (String) 
<submission type>: "file", "dir", atau "zip" 
<submission language>: "java", "py", "web", "dart", atau "csharp". 
<explanation language>: "en" atau "id" 
<minimum similarity threshold>: bilangan bulat tidak negatif 0-100 
<maximum reported submission pairs>: bilangan bulat positif
<minimum matching length>: bilangan bulat positif tidak kurang dari 2 
<similarity measurement>: "minhash", "super-bit", "jaccard", "cosine", "rkrgst", "sensitive minhash", "sensitive super-bit", "sensitive jaccard", "sensitive cosine", atau "sensitive rkrgst"  
<resource path>: path direktori absolut ke direktori resource SSTRANGE (String)
<dissimilarity threshold>: bilangan bulat tidak negatif 0-100
<AI submission path>: path direktori absolut (String) 
<number of clusters>: bilangan bulat positif tidak kurang dari 2 (hanya untuk minhash dan super-bit)
<number of stages>: bilangan bulat positif (hanya untuk minhash dan super-bit)
```
