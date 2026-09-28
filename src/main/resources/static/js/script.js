const navLinks = document.querySelectorAll(".nav-link");
const panels = document.querySelectorAll(".tab-panel");
const menuToggle = document.querySelector(".menu-toggle");
const mainNav = document.querySelector(".main-nav");

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

  mainNav.classList.remove("open");
  menuToggle.setAttribute("aria-expanded", "false");

  if (updateHistory) {
    history.pushState(null, "", `#${tabId}`);
  }

  window.scrollTo({ top: 0, behavior: "smooth" });
}

navLinks.forEach((link) => {
  link.addEventListener("click", () => showTab(link.dataset.tab));
});

menuToggle.addEventListener("click", () => {
  const isOpen = mainNav.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(isOpen));
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

const contactForm = document.getElementById("contact-form");
if (contactForm) {
  contactForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const name = form.elements.name.value.trim();
    form.querySelector(".form-status").textContent = `Thank you${name ? `, ${name}` : ""}. Your message is ready to send.`;
    form.reset();
  });
}

document.getElementById("year").textContent = new Date().getFullYear();
