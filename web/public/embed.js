// Fits OpenWalrus grunt embeds on a page to their content. Each embed reports its height;
// only frames that are OpenWalrus pages, at the address the message came from, are resized.
// This script may load before or after the embeds, so it also asks each one to report
// again: now, and when its frame finishes loading.
(function () {
  if (window.openwalrusEmbeds) return;
  window.openwalrusEmbeds = true;
  var script = document.currentScript;
  var origin = script ? new URL(script.src).origin : null;

  function originOf(frame) {
    try {
      return new URL(frame.src, location.href).origin;
    } catch (_) {
      return null;
    }
  }

  window.addEventListener("message", function (e) {
    var data = e.data;
    if (!data || data.type !== "openwalrus:height" || typeof data.height !== "number") return;
    var frames = document.getElementsByTagName("iframe");
    for (var i = 0; i < frames.length; i++) {
      if (frames[i].contentWindow === e.source && originOf(frames[i]) === e.origin) {
        frames[i].style.height = Math.ceil(data.height) + "px";
      }
    }
  });

  function ask(frame) {
    if (frame.contentWindow) frame.contentWindow.postMessage({ type: "openwalrus:measure" }, origin);
  }

  var frames = document.getElementsByTagName("iframe");
  for (var i = 0; i < frames.length; i++) {
    if (!origin || originOf(frames[i]) !== origin) continue;
    ask(frames[i]);
    frames[i].addEventListener("load", ask.bind(null, frames[i]));
  }
})();
