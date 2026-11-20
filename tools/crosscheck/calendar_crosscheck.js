#!/usr/bin/env node
// So lịch âm của :core:calendar với amlich.js (port JavaScript nguyên bản thuật toán Hồ Ngọc Đức).
//
//   npm install --prefix /tmp/amlich amlich.js
//   ./gradlew :core:calendar:dumpLunar -q > /tmp/lunar.txt
//   NODE_PATH=/tmp/amlich/node_modules node tools/crosscheck/calendar_crosscheck.js /tmp/lunar.txt
//
// Kỳ vọng: chỉ lệch đúng 7 ngày mà bản gốc trả về “ngày 0” (docs/DECISIONS.md D-03).
const fs = require("fs");
const amlich = require("amlich.js");

const lines = fs.readFileSync(process.argv[2] || "/tmp/lunar.txt", "utf8").trim().split("\n");
let diffs = 0;
for (const line of lines) {
  const [d, m, y, tz, ld, lm, ly, leap] = line.split(" ").map(Number);
  const r = amlich.computeDateToLunarDate(d, m, y, tz);
  if (r.lunarDay !== ld || r.lunarMonth !== lm || r.lunarYear !== ly || (r.lunarLeap ? 1 : 0) !== leap) {
    diffs++;
    console.log(`Lệch ${d}/${m}/${y} tz${tz}: engine ${ld}/${lm}${leap ? "N" : ""}/${ly}, amlich.js ${r.lunarDay}/${r.lunarMonth}${r.lunarLeap ? "N" : ""}/${r.lunarYear}`);
  }
}
console.log(`Đã so ${lines.length} ngày, lệch ${diffs}.`);
