# Networking Project 3

# Authors
- Adam Walton
- Anton Ryan
- Jacob Smith


SERVER HELP
java -cp "out;lib/merrimackutil.jar" server.Server --help

CLIENT HELP
java -cp "out;lib/merrimackutil.jar" client.Client --help




RUN SERVER
java -cp "out;lib/merrimackutil.jar" server.Server --config config-2.json

GET file.txt
java -cp "out;lib/merrimackutil.jar" client.Client --get file.txt --server 127.0.0.1:5000

PUT greeting.txt
Set-Content -NoNewline -Path .\greeting.txt -Value "hi from client"
java -cp "out;lib\merrimackutil.jar" client.Client --put greeting.txt --server 127.0.0.1:5000
Get-Content .\data\greeting.txt

CREATE FILE WITH 512 BYTES
$r = "A" * 512
Set-Content -NoNewline -Path .\exact512.txt -Value $r

UPLOAD
java -cp "out;lib/merrimackutil.jar" client.Client --put exact512.txt --server 127.0.0.1:5000

DOWNLOAD
java -cp "out;lib/merrimackutil.jar" client.Client --get exact512.txt --server 127.0.0.1:5000
