<#import "template.ftl" as layout>
<@layout.emailLayout>
    <h1 style="color: #333; font-size: 24px; margin-bottom: 20px; text-align: center;">Vérifiez votre adresse email</h1>

    <p>Bonjour,</p>

    <p>Merci de vous être inscrit à QYPYM. Pour valider votre compte, veuillez utiliser le code suivant :</p>

    <div style="text-align: center; margin: 20px 0;">
        <span style="font-size: 32px; font-weight: bold; color: #E59C00;">${code}</span>
    </div>

    <p>Ce code expirera dans ${codeExpiration} minutes.</p>

    <p style="color: #777; font-size: 13px; margin-top: 30px;">Si vous n'avez pas demandé ce code, veuillez ignorer cet email.</p>

    <p style="margin-top: 30px;">Cordialement,<br>L'équipe QYPYM</p>
</@layout.emailLayout>
