#!/bin/zsh
set -e
DOCX="/Users/himanshu/Documents/hospital/Smart_Hospital_Project_Report_Formatted.docx"
PDF="/Users/himanshu/Documents/hospital/Smart_Hospital_Project_Report_Formatted.pdf"
open -a Pages "$DOCX"
sleep 15
osascript <<APPLESCRIPT
set outputPath to POSIX file "$PDF"
tell application "Pages"
    set theDoc to front document
    export theDoc to outputPath as PDF
    close theDoc saving no
end tell
APPLESCRIPT
