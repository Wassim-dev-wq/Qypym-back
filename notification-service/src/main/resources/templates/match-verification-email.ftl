<#import "template.ftl" as layout>
<@layout.emailLayout>
    <h1 style="color: #333; font-size: 24px; margin-bottom: 20px; text-align: center;">Code de vérification pour les joueurs</h1>

    <p>Bonjour ${firstName},</p>

    <p>Voici le code de vérification pour votre match <strong>${matchTitle}</strong> :</p>

    <div style="text-align: center; margin: 20px 0;">
        <span style="font-size: 32px; font-weight: bold; color: #E59C00;">${verificationCode}</span>
    </div>

    <p>Ce code est à partager avec vos joueurs afin qu'ils puissent valider leur présence au match.</p>

    <div style="background-color: #f8f8f8; padding: 15px; border-left: 4px solid #E59C00; margin: 20px 0;">
        <p style="margin: 0;"><strong>Important :</strong> Veuillez communiquer ce code uniquement aux joueurs participant à ce match.</p>
    </div>

    <h2 style="color: #333; font-size: 20px; margin-top: 20px;">Détails du match :</h2>
    <ul style="list-style-type: none; padding-left: 0;">
        <li><strong>Date et heure :</strong> ${matchDate}</li>
        <li><strong>Lieu :</strong> ${matchLocation}</li>
        <li><strong>Format :</strong> ${matchFormat}</li>
        <#if teamName??>
            <li><strong>Équipe :</strong> ${teamName}</li>
        </#if>
    </ul>

    <div style="margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee;">
        <p>Informations supplémentaires :</p>
        <ul style="list-style-type: none; padding-left: 0;">
            <li>• Ce code est valide pendant ${codeValidityMinutes} minutes</li>
            <li>• Les joueurs doivent utiliser ce code pour confirmer leur présence</li>
            <li>• Vous pouvez demander un nouveau code si celui-ci expire</li>
        </ul>
    </div>

    <p style="color: #777; font-size: 13px; margin-top: 30px;">Si vous n'êtes pas le propriétaire de ce match, veuillez ignorer cet email.</p>

    <p style="margin-top: 30px;">Cordialement,<br>L'équipe QYPYM</p>
</@layout.emailLayout>