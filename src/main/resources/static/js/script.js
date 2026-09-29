const navLinks = document.querySelectorAll(".nav-link");
const panels = document.querySelectorAll(".tab-panel");
const menuToggle = document.querySelector(".menu-toggle");
const mainNav = document.querySelector(".main-nav");

function setMenuOpen(isOpen) {
  mainNav.classList.toggle("open", isOpen);
  menuToggle.setAttribute("aria-expanded", String(isOpen));
  document.body.classList.toggle("menu-open", isOpen);
}

function showTab(tabId, updateHistory = true) {
  navLinks.forEach((link) => {
    const isActive = link.dataset.tab === tabId;
    link.classList.toggle("active", isActive);
    link.setAttribute("aria-selected", String(isActive));
  });

  panels.forEach((panel) => {
    const isActive = panel.id === tabId;
    panel.hidden = !isActive;
    panel.classList.toggle("active", isActive);
  });

  setMenuOpen(false);

  if (updateHistory) {
    history.pushState(null, "", `#${tabId}`);
  }

  window.scrollTo({ top: 0, behavior: "smooth" });
}

navLinks.forEach((link) => {
  link.addEventListener("click", () => showTab(link.dataset.tab));
});

document.querySelectorAll('a[href="#works"]').forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
    showTab("works");
  });
});

menuToggle.addEventListener("click", () => {
  setMenuOpen(!mainNav.classList.contains("open"));
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && mainNav.classList.contains("open")) {
    setMenuOpen(false);
    menuToggle.focus();
  }
});

window.addEventListener("resize", () => {
  if (window.innerWidth > 760 && mainNav.classList.contains("open")) {
    setMenuOpen(false);
  }
});

window.addEventListener("popstate", () => {
  const tabId = location.hash.slice(1);
  if (document.getElementById(tabId)?.classList.contains("tab-panel")) {
    showTab(tabId, false);
  }
});

const initialTab = location.hash.slice(1);
if (document.getElementById(initialTab)?.classList.contains("tab-panel")) {
  showTab(initialTab, false);
}

document.getElementById("year").textContent = new Date().getFullYear();
