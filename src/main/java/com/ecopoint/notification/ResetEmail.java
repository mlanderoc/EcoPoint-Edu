package com.ecopoint.notification;

import com.ecopoint.model.User;

/**
 * Text of the email with the link to create a new password (spec D18, RP-09).
 * It has no password and no other data of the account.
 *
 * @param subject subject line
 * @param text    plain-text body
 * @param html    HTML body
 * @author mlanderoc
 * @version 1.0
 */
public record ResetEmail(String subject, String text, String html) {

    /** Path of the frontend page that receives the code. */
    public static final String RESET_PATH = "/restablecer-contrasena?token=";

    private static final String SUBJECT = "Crea tu nueva contraseña de EcoPoint Edu";

    /**
     * Builds the email for a user.
     *
     * @param user           recipient
     * @param frontendOrigin public address of the frontend
     * @param code           single-use code of the link
     * @return the email
     */
    public static ResetEmail forUser(User user, String frontendOrigin, String code) {
        String link = link(frontendOrigin, code);
        String text = "Hola, " + user.getFirstName() + ".\n\n"
                + "Recibimos una solicitud para cambiar tu contraseña de EcoPoint Edu. "
                + "Abre este enlace para crear una nueva:\n\n" + link + "\n\n"
                + "El enlace vence en 30 minutos y solo sirve una vez.\n"
                + "Si no lo pediste, ignora este correo: tu contraseña no cambia.\n";
        String html = "<p>Hola, " + escape(user.getFirstName()) + ".</p>"
                + "<p>Recibimos una solicitud para cambiar tu contraseña de EcoPoint Edu.</p>"
                + "<p><a href=\"" + escape(link) + "\" style=\"display:inline-block;padding:10px 18px;"
                + "background:#1E8449;color:#ffffff;border-radius:6px;text-decoration:none\">"
                + "Crear contraseña nueva</a></p>"
                + "<p>El enlace vence en 30 minutos y solo sirve una vez.</p>"
                + "<p>Si no lo pediste, ignora este correo: tu contraseña no cambia.</p>";
        return new ResetEmail(SUBJECT, text, html);
    }

    /**
     * Builds the link of the email.
     *
     * @param frontendOrigin public address of the frontend
     * @param code           single-use code, already safe for a URL
     * @return the full link
     */
    public static String link(String frontendOrigin, String code) {
        String origin = frontendOrigin.endsWith("/")
                ? frontendOrigin.substring(0, frontendOrigin.length() - 1)
                : frontendOrigin;
        return origin + RESET_PATH + code;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
