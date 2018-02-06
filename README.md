# dataset-tools
Java command-line tools for comparing results to ground truth for table location and structure detection as used in the ICDAR 2013 Table Competition.

## Usage

Download the build (JAR file). The command-line takes a minimum of 2 and a maximum of 5 parameters as input.

The first parameter is always `-reg` or `-str`, depending on whether you are comparing region detection or structure recognition results respectively.

The second parameter is the prefix of the files. If they are all in the same directory and named according to the convention specified in the ICDAR 2013 competition, only these two parameters are necessary.

For example, to compare region detection results for the file `eu-001.pdf` with the ground truth file `eu-001-reg.xml` and the algorithm's result in `eu-001-reg-result.xml`, you would run the following command:

`java -jar dataset-tools-2018XXXX.jar -reg eu-001`

If the algorithm's result has a different suffix or completely different filename, a third parameter is necessary. For example, if this file is called `eu-001-reg-alg-output.xml`, you would run:

`java -jar dataset-tools-2018XXXX.jar -reg eu-001 eu-001-reg-alg-output.xml`

Finally, you can use the fourth parameter to specify a different name or location for the PDF file. For example, if all PDFs are in the subdirectory `PDFs` you would type:

`java -jar dataset-tools-2018XXXX.jar -reg eu-001 eu-001-reg-alg-output.xml pdfs/eu-001.pdf`

The same applies also to structure recognition results, just replace `-reg` with `-str`.

If you run from the compiled directory instead of the JAR, use the script `measure-recognition-performance.sh` instead:

`./measure-recognition-performance.sh -reg eu-001`
