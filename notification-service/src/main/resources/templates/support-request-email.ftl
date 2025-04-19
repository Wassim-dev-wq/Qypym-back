<#import "template.ftl" as layout>
<@layout.emailLayout>
    <h1 style="color: #333; font-size: 24px; margin-bottom: 20px; text-align: center;">Nouvelle demande de support</h1>

    <div style="background-color: #f8f8f8; border-left: 4px solid #E59C00; padding: 15px; margin-bottom: 20px;">
        <p style="font-weight: bold; margin-bottom: 5px;">Catégorie: ${request.category}</p>
        <p style="font-weight: bold; margin-bottom: 5px;">Sujet: ${request.subject}</p>
        <p style="font-style: italic; color: #666; margin-bottom: 0;">Date: ${timestamp?string["dd MMMM yyyy, HH:mm"]}</p>
    </div>

    <h2 style="color: #444; font-size: 18px; margin-bottom: 10px;">Informations utilisateur</h2>
    <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
        <tr>
            <td style="padding: 8px; border-bottom: 1px solid #eee; width: 30%;"><strong>ID:</strong></td>
            <td style="padding: 8px; border-bottom: 1px solid #eee;">${request.userId}</td>
        </tr>
        <tr>
            <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Nom:</strong></td>
            <td style="padding: 8px; border-bottom: 1px solid #eee;">${request.firstName!""} ${request.lastName!""}</td>
        </tr>
        <tr>
            <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Email:</strong></td>
            <td style="padding: 8px; border-bottom: 1px solid #eee;"><a href="mailto:${request.email!""}">${request.email!""}</a></td>
        </tr>
        <#if request.username?? && request.username?has_content>
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>Nom d'utilisateur:</strong></td>
                <td style="padding: 8px; border-bottom: 1px solid #eee;">@${request.username}</td>
            </tr>
        </#if>
    </table>

    <h2 style="color: #444; font-size: 18px; margin-bottom: 10px;">Message</h2>
    <div style="background-color: #f8f8f8; padding: 20px; border-radius: 5px; margin-bottom: 20px; white-space: pre-wrap;">
        ${request.message?replace('\n', '<br>')}
    </div>

    <p style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 14px;">
        Ce message a été envoyé depuis l'application mobile QYPYM. Vous pouvez répondre directement à cet email pour contacter l'utilisateur.
    </p>
</@layout.emailLayout>