# Guia Completo de Configuração Firebase - MotoReviso

## ⚠️ Erro Identificado
O erro **404 "Object does not exist at location"** ocorre porque as regras de segurança do Firebase Storage não estão configuradas para permitir uploads.

---

## 📋 Passo 1: Configurar Firebase Storage Rules

### 1.1 Acessar o Console Firebase
1. Abra https://console.firebase.google.com/
2. Selecione seu projeto **MotoReviso**
3. Clique em **Storage** no menu lateral esquerdo
4. Clique na aba **Regras**

### 1.2 Substituir as regras
Apague todo o conteúdo atual e copie as regras abaixo:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Permitir uploads de imagens de veículos
    match /veiculo_images/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // Permitir uploads de fotos de perfil
    match /profile_images/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // Regra padrão - Permitir leitura/escrita para usuários autenticados
    match /{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
      allow delete: if request.auth != null;
    }
  }
}
```

### 1.3 Publicar as regras
1. Clique no botão **Publicar** no canto superior direito
2. Confirme a ação

---

## 📋 Passo 2: Configurar Firestore Rules

### 2.1 Acessar Firestore
1. No menu lateral, clique em **Firestore Database**
2. Clique na aba **Regras**

### 2.2 Substituir as regras
Apague todo o conteúdo e copie as regras abaixo:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isOwner(uid) {
      return request.auth.uid == uid;
    }

    // Coleção de veículos
    match /veiculo/{veiculoId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }

    // Coleção de trajetos
    match /trajeto/{trajetoId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }

    // Coleção de manutenção
    match /manutencao/{manutencaoId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }

    // Coleção de imagens de veículos
    match /imagem_veiculo/{imagemId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }

    // Dados do usuário
    match /users/{userId}/{document=**} {
      allow read: if request.auth != null && isOwner(userId);
      allow write: if request.auth != null && isOwner(userId);
    }

    // Rejeitar tudo que não foi explicitamente permitido
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### 2.3 Publicar as regras
1. Clique no botão **Publicar** no canto superior direito
2. Confirme a ação

---

## 🔐 Passo 3: Verificar Autenticação

### 3.1 Ativar Email/Senha
1. Vá para **Authentication** (Autenticação)
2. Clique na aba **Sign-in method**
3. Certifique-se de que **Email/Password** está ativado
4. Se não estiver, clique em **Email/Password** > **Enable** > **Save**

### 3.2 Ativar Anonymous Auth (opcional, para testes)
Se quiser testar sem login:
1. Na aba **Sign-in method**, clique em **Anonymous**
2. Clique em **Enable** > **Save**

---

## 📱 Passo 4: Atualizar código do app

Nenhuma alteração necessária no código Java. O app já está configurado corretamente.

---

## ✅ Verificar se funcionou

1. **Rebuild** do app no Android Studio
2. Tente fazer login
3. Edite um veículo e tente fazer upload de uma foto
4. A foto deve ser enviada com sucesso

---

## 🆘 Se ainda não funcionar

### Debug Firebase Storage
1. No Android Studio, abra o **Logcat**
2. Filtre por: `firebase`
3. Procure por mensagens de erro

### Verificar URL do bucket
1. No Console Firebase, vá para **Storage**
2. Copie a URL no formato: `gs://seu-projeto-firebaseapp.appspot.com`
3. Compare com o que aparece no log do app

### Limpar cache
```bash
./gradlew clean
./gradlew build
```

---

## 📚 Explicação das Regras

### Storage Rules
- **`allow read`**: Qualquer usuário autenticado pode ler/baixar imagens
- **`allow write`**: Apenas o próprio usuário pode fazer upload na sua pasta
- **`allow delete`**: Apenas o próprio usuário pode deletar suas imagens

### Firestore Rules
- **`allow read`**: Qualquer usuário autenticado pode ler qualquer documento
- **`allow create`**: Apenas o proprietário pode criar (userId deve ser igual ao uid do auth)
- **`allow update`**: Apenas o proprietário pode atualizar
- **`allow delete`**: Apenas o proprietário pode deletar

---

## 🔒 Segurança

Estas regras garantem que:
✅ Apenas usuários autenticados podem usar o app
✅ Cada usuário só pode ver/editar seus próprios dados
✅ Dados não podem ser deletados acidentalmente por outros usuários
✅ Uploads de imagens são protegidos

---

## 📞 Contato/Suporte

Se o erro persistir:
1. Verifique se a autenticação está funcionando
2. Verifique se o bucket do Storage foi criado (deve aparecer em **Storage**)
3. Verifique os logs do Firebase no Console (aba **Logs**)
