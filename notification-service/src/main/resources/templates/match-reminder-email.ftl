<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Rappel : Votre match commence bientôt</title>
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
<div style="background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
    <h1 style="color: #2c3e50; margin-top: 0;">Rappel : Votre match commence bientôt</h1>
    <p>Bonjour ${firstName},</p>
    <p>Votre match <strong>${matchTitle}</strong> commence dans moins de 5 heures !</p>

    <h2 style="color: #2c3e50; margin-top: 20px;">Détails du match :</h2>
    <ul style="list-style-type: none; padding-left: 0;">
        <li><strong>Date et heure :</strong> ${matchDate}</li>
        <li><strong>Lieu :</strong> ${matchLocation}</li>
        <li><strong>Format :</strong> ${matchFormat}</li>
        <#if teamName??>
            <li><strong>Équipe :</strong> ${teamName}</li>
        </#if>
        <#if playerRole??>
            <li><strong>Rôle :</strong> ${playerRole}</li>
        </#if>
    </ul>

    <p style="background-color: #e9ecef; padding: 10px; border-left: 4px solid #007bff; margin: 20px 0;">
        N'oubliez pas d'apporter votre équipement et d'arriver à l'heure !
    </p>
</div>

<p style="font-size: 12px; color: #6c757d; margin-top: 30px; text-align: center;">
    Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
</p>
</body>
</html>