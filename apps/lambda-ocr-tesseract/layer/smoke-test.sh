#!/bin/sh
set -eu
# // given
# Render a tiny portable bitmap so the smoke test exercises recognition as well as loading.
awk 'BEGIN {
  glyph[0]="10001100011000111111100011000110001";
  glyph[1]="11111100001000011110100001000011111";
  glyph[2]="10000100001000010000100001000011111";
  glyph[3]=glyph[2];
  glyph[4]="01110100011000110001100011000101110";
  width=220; height=82; print "P1"; print width, height;
  for (y=0; y<height; y++) {
    row="";
    for (x=0; x<width; x++) {
      pixel=0;
      if (x>=20 && x<200 && y>=20 && y<62) {
        letter=int((x-20)/36); col=int(((x-20)%36)/6); line=int((y-20)/6);
        if (col<5) pixel=substr(glyph[letter], line*5+col+1, 1);
      }
      row=row pixel " ";
    }
    print row;
  }
}' > /tmp/ocr-smoke.pbm
# // when
version=$(/opt/bin/tesseract --version 2>&1)
languages=$(/opt/bin/tesseract --list-langs)
recognized=$(/opt/bin/tesseract /tmp/ocr-smoke.pbm stdout --psm 7 -l eng)
# // then
printf '%s\n' "$version" | grep '^tesseract 5.5.2'
printf '%s\n' "$languages" | grep -Fx eng
printf '%s\n' "$languages" | grep -Fx osd
printf '%s\n' "$recognized" | grep -Fx HELLO
