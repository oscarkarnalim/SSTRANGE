# SSTRANGE: Scalable Similarity TRacker in Academia with Natural lanGuage Explanation
**SSTRANGE** \(Scalable Similarity TRacker in Academia with Natural lanGuage Explanation\) is a scalable and efficient tool to observe similarities among submissions with locality sensitive hashing: MinHash and Super-Bit. Currently, the tool supports Java and Python submissions. Further details can be seen in [the corresponding paper](https://www.mdpi.com/2227-7102/13/1/54) published in MDPI's Education Sciences as part of special issue "Application of New Technologies for Assessment in Higher Education".

Unlike its counterpart, [Comprehensive STRANGE](https://github.com/oscarkarnalim/CSTRANGE), SSTRANGE focuses on efficiency and it is suitable for large submissions. For comprehensive reporting, it is recommended to use [Comprehensive STRANGE or CSTRANGE](https://github.com/oscarkarnalim/CSTRANGE) instead.

SSTRANGE and CSTRANGE has comparable features: graphical user interface, template code removal, and common code removal. 

## User Interface
### Main layout

### Investigation report

### Comparison report

## Input 
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

### Template directory path
This refers to a directory containing tenplate content stored as submission files. The tool will try to remove all template content in student submissions prior comparison.

### Common content
If this feature is turned on, the tool will try to remove all similar contents that are common among student submissions. This might result in longer processing time.

### Similarity measurement
How similarities will be detected. The options are MinHash, Super-Bit, Jaccard, Cosine, and RKRGST (running Karp-Rabin greedy string tiling). MinHash and Super-Bit are the most time efficient while RKRGST is the slowest.

## Acknowledgments
This tool uses [STRANGE](https://github.com/oscarkarnalim/strange) as the basis of development, [CSTRANGE](https://github.com/oscarkarnalim/strange) to develop user interface, [ANTLR](https://www.antlr.org/) to tokenise given submissions, [tdebatty's module](https://github.com/tdebatty/java-LSH) to do locality sensitive hashing, and [Google Prettify](https://github.com/google/code-prettify) to display source code.
