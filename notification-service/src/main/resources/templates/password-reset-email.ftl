<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Réinitialisation de votre mot de passe</title>
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
<div style="background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
    <h1 style="color: #2c3e50; margin-top: 0;">Réinitialisation de votre mot de passe</h1>
    <p>Bonjour,</p>
    <p>Vous avez demandé la réinitialisation de votre mot de passe. Voici votre code de vérification :</p>

    <div style="background-color: #e9ecef; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0; border-radius: 5px;">
        ${resetCode}
    </div>

    <p>Ce code est valide pendant ${expirationMinutes} minutes. Après cette période, vous devrez demander un nouveau code.</p>

    <p>Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email et votre mot de passe restera inchangé.</p>

    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
        <p>Pour des raisons de sécurité :</p>
        <ul style="list-style-type: none; padding-left: 0;">
            <li>• Ne partagez jamais ce code avec qui que ce soit</li>
            <li>• Notre équipe ne vous demandera jamais ce code par téléphone ou email</li>
            <li>• Utilisez un mot de passe fort et unique</li>
        </ul>
    </div>
</div>

<p style="font-size: 12px; color: #6c757d; margin-top: 30px; text-align: center;">
    Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
</p>
</body>
</html>