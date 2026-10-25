'use strict';
const boxes = [...document.querySelectorAll('[data-step]')];
const progress = document.querySelector('#progress');
function update() {
  progress.textContent = `${boxes.filter(box => box.checked).length} of 5 steps complete`;
}
for (const box of boxes) {
  try { box.checked = localStorage.getItem(`nuttyvr.setup.${box.dataset.step}`) === 'done'; } catch (_) { /* Checklist also works without persistence. */ }
  box.addEventListener('change', () => {
    try { localStorage.setItem(`nuttyvr.setup.${box.dataset.step}`, box.checked ? 'done' : ''); } catch (_) { /* Storage is optional. */ }
    update();
  });
}
document.querySelector('#reset').addEventListener('click', () => {
  for (const box of boxes) {
    box.checked = false;
    try { localStorage.removeItem(`nuttyvr.setup.${box.dataset.step}`); } catch (_) { /* Storage is optional. */ }
  }
  update();
});
update();
