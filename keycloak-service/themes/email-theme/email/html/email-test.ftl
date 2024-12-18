<#import "template.ftl" as layout>
<@layout.emailLayout>
<html>
<head>
    <style>
        /* General styling */
        body {
            font-family: Arial, sans-serif;
            color: #333333;
            margin: 0;
            padding: 0;
        }
        .email-container {
            width: 100%;
            max-width: 600px;
            margin: 0 auto;
            border: 1px solid #dddddd;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
        }
        .header {
            background-color: #0044cc;
            color: white;
            padding: 20px;
            text-align: center;
        }
        .content {
            padding: 20px;
            line-height: 1.6;
        }
        .content p {
            margin: 0 0 15px;
        }
        .button {
            display: inline-block;
            padding: 12px 20px;
            margin: 20px 0;
            background-color: #0044cc;
            color: white;
            text-decoration: none;
            border-radius: 5px;
        }
        .footer {
            font-size: 12px;
            color: #888888;
            text-align: center;
            padding: 10px;
            background-color: #f9f9f9;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <h1>Bienvenue à Fivy</h1>
        </div>
        <div class="content">
            <p>Bonjour,</p>
            <p>Nous vous remercions d'avoir créé un compte sur Fivy. Afin de compléter votre inscription, veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous.</p>
            <p><a href="https://example.com/verify-email" class="button">Confirmer mon adresse email</a></p>
            <p>Si vous ne pouvez pas cliquer sur le lien, veuillez copier et coller l'URL suivante dans votre navigateur :</p>
            <p>https://example.com/verify-email</p>
            <p>Ce lien expirera dans 24 heures.</p>
            <p>Merci de faire partie de notre communauté!</p>
        </div>
        <div class="footer">
            <p>© 2024 Fivy. Tous droits réservés.</p>
        </div>
    </div>
</body>
</html>
</@layout.emailLayout>
