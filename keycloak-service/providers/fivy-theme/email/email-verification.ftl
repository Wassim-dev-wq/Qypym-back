<html>
<head>
    <title>Vérification de votre adresse email</title>
</head>
<body>
    <p>Bonjour ${user.firstName} ${user.lastName},</p>
    
    <p>Merci de vous être inscrit sur l'application Fivy ! Pour finaliser votre inscription, veuillez confirmer votre adresse email en cliquant sur le lien ci-dessous :</p>
    
    <p>
        <a href="${link}">Confirmer mon adresse email</a>
    </p>
    
    <p>Si vous ne parvenez pas à cliquer sur le lien, vous pouvez copier et coller l'URL suivante dans votre navigateur :</p>
    
    <p>${link}</p>
    
    <p>Ce lien de vérification expirera dans 24 heures.</p>
    
    <p>Merci et à bientôt sur Fivy,<br>
    L'équipe Fivy</p>
</body>
</html>
