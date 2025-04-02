<#macro emailLayout>
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>QYPYM</title>
    </head>
    <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 16px; line-height: 1.5; margin: 0; padding: 0; background-color: #f8f8f8; color: #333;">
    <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #f8f8f8;">
        <tr>
            <td align="center">
                <table border="0" cellpadding="0" cellspacing="0" width="600" style="margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);">
                    <tr>
                        <td align="center" style="padding: 30px 0;">
                            <div style="font-size: 32px; font-weight: bold; color: #E59C00;">QYPYM</div>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 40px;">
                            <#nested>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" style="padding: 30px 0;">
                            <p style="color: #777; font-size: 13px; margin: 0;">
                                © ${.now?string('yyyy')} QYPYM. Tous droits réservés.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
    </body>
    </html>
</#macro>