<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Code de vérification pour votre match</title>
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
<div style="background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
    <h1 style="color: #2c3e50; margin-top: 0;">Code de vérification pour votre match</h1>
    <p>Bonjour ${firstName},</p>
    <p>Voici le code de vérification pour votre match <strong>${matchTitle}</strong> :</p>

    <div style="background-color: #e9ecef; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0; border-radius: 5px;">
        ${verificationCode}
    </div>

    <p>Ce code sera valide jusqu'à ${codeValidityMinutes} minutes après le début du match.</p>

    <h2 style="color: #2c3e50; margin-top: 20px;">Détails du match :</h2>
    <ul style="list-style-type: none; padding-left: 0;">
        <li><strong>Date et heure :</strong> ${matchDate}</li>
        <li><strong>Lieu :</strong> ${matchLocation}</li>
        <li><strong>Format :</strong> ${matchFormat}</li>
    </ul>

    <p>Vous pouvez utiliser ce code pour confirmer votre présence et celle des autres joueurs.</p>
</div>

<p style="font-size: 12px; color: #6c757d; margin-top: 30px; text-align: center;">
    Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
</p>
</body>
</html>