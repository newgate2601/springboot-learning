@echo off
cd /d C:\code\english
echo === %date% %time% > push-wordgarden.log
git add 06-wordgarden-ios .github/workflows/wordgarden-ios.yml >> push-wordgarden.log 2>&1
git commit -m "WordGarden: publish IPA to release" >> push-wordgarden.log 2>&1
git push origin learn-english >> push-wordgarden.log 2>&1
echo EXIT=%errorlevel% >> push-wordgarden.log
