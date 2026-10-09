// Pede confirmação antes de enviar formulários marcados com data-confirmar.
// Fica em arquivo separado porque a Content-Security-Policy proíbe scripts inline.
document.querySelectorAll('form[data-confirmar]').forEach(function (form) {
    form.addEventListener('submit', function (evento) {
        if (!window.confirm(form.dataset.confirmar)) {
            evento.preventDefault();
        }
    });
});
