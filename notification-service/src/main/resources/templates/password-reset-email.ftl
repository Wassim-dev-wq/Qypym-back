<#import "template.ftl" as layout>
<@layout.emailLayout>
    <h1 style="color: #333; font-size: 24px; margin-bottom: 20px; text-align: center;">Réinitialisation de votre mot de passe</h1>

    <p>Bonjour,</p>

    <p>Vous avez demandé la réinitialisation de votre mot de passe. Voici votre code de vérification :</p>

    <div style="text-align: center; margin: 20px 0;">
        <span style="font-size: 32px; font-weight: bold; color: #E59C00;">${resetCode}</span>
    </div>

    <p>Ce code est valide pendant ${expirationMinutes} minutes. Après cette période, vous devrez demander un nouveau code.</p>

    <div style="margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee;">
        <p>Pour des raisons de sécurité :</p>
        <ul style="list-style-type: none; padding-left: 0;">
            <li>• Ne partagez jamais ce code avec qui que ce soit</li>
            <li>• Notre équipe ne vous demandera jamais ce code par téléphone ou email</li>
            <li>• Utilisez un mot de passe fort et unique</li>
        </ul>
    </div>

    <p style="color: #777; font-size: 13px; margin-top: 30px;">Si vous n'avez pas demandé ce code, veuillez ignorer cet email.</p>

    <p style="margin-top: 30px;">Cordialement,<br>L'équipe QYPYM</p>
</@layout.emailLayout>