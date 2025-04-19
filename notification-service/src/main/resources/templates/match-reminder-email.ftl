<#import "template.ftl" as layout>
<@layout.emailLayout>
    <h1 style="color: #333; font-size: 24px; margin-bottom: 20px; text-align: center;">Rappel : Votre match commence bientôt</h1>

    <p>Bonjour ${firstName},</p>

    <p>Votre match <strong>${matchTitle}</strong> commence dans moins de 5 heures !</p>

    <h2 style="color: #333; font-size: 20px; margin-top: 20px;">Détails du match :</h2>
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

    <div style="background-color: #f8f8f8; padding: 15px; border-left: 4px solid #E59C00; margin: 20px 0;">
        <p style="margin: 0;">N'oubliez pas d'apporter votre équipement et d'arriver à l'heure !</p>
    </div>

    <p style="color: #777; font-size: 13px; margin-top: 30px;">Si vous n'avez pas inscrit à ce match, veuillez ignorer cet email.</p>

    <p style="margin-top: 30px;">Cordialement,<br>L'équipe QYPYM</p>
</@layout.emailLayout>