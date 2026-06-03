# Regras de Firebase Storage Simplificadas

O código foi modificado para usar um caminho simples: **`vehicles/{filename}`**

## Regras para Copiar e Colar

Vá ao **Firebase Console → Storage → Regras** e substitua TUDO por isto:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Permitir leitura para qualquer um
    match /{allPaths=**} {
      allow read: if true;
    }
    
    // Permitir escrita apenas para usuários autenticados
    match /vehicles/{fileName} {
      allow write: if request.auth != null;
    }
    
    // Negar outras escritas
    match /{allPaths=**} {
      allow write: if false;
    }
  }
}
```

## Passos:

1. Abra [Firebase Console](https://console.firebase.google.com/)
2. Projeto: **motoreviso**
3. Esquerda → **Storage**
4. Clique na aba **Regras**
5. Selecione TUDO (Ctrl+A)
6. Cole as regras acima
7. Clique **Publicar**

## Estrutura de Armazenamento

Depois disso, as imagens serão organizadas assim:

```
Firebase Storage (motoreviso bucket)
└─ vehicles/
   ├─ veiculo_FfAoAg2xkyx2lRVhbL4A_1780437466481.jpg
   ├─ veiculo_XYZ789_1780437500000.jpg
   └─ ... mais imagens
```

Simples e funcional! ✓

## Mudanças no Código

O método `uploadImagemVeiculo()` em `FirebaseManager.java` foi modificado para:

**Antes:**
```java
storage.getReference()
    .child("veiculo_images")  // Pasta intermediária
    .child(userId)            // Pasta do usuário
    .child(nomeArquivo)       // Nome do arquivo
```

**Depois:**
```java
storage.getReference()
    .child("vehicles")        // Pasta raiz
    .child(nomeArquivo)       // Nome do arquivo direto
```

Muito mais simples! 

## Por que funciona melhor?

1. ✅ Menos pastas = menos problemas com 404
2. ✅ Estrutura mais simples de gerenciar
3. ✅ Regras de segurança mais diretas
4. ✅ Funciona com qualquer configuração de bucket

## Testes

Depois de atualizar as regras:

1. **Reconstruir**: `Build → Make Project`
2. **Executar**: App
3. **Editar Veículo** → Alterar Foto → Salvar
4. **Verificar logs**:
   - Deve aparecer: `Iniciando upload para: vehicles/veiculo_...`
   - Depois: `Upload concluído, obtendo URL de download...`
   - Depois: `URL de download obtida: https://firebasestorage.googleapis.com/...`
   - Por fim: `Veículo atualizado com sucesso!`

Se ver tudo isso = **SUCESSO!** 🎉

## Troubleshooting

**Se ainda receber 404:**

Verifique no Firebase Console:
1. Vá para **Storage**
2. Verifique se existe um bucket (deve mostrar `gs://motoreviso...`)
3. Se não existir, clique "Get Started" e crie um novo bucket
4. Escolha localização: `us-central1`
5. Aguarde criação (~1 minuto)
6. Depois atualize as Regras

**Se receber 403 (Permission denied):**

Significa as regras ainda não estão corretas:
1. Volte para **Storage → Regras**
2. Verifique se as regras foram publicadas com sucesso (deve aparecer "Publicadas com sucesso")
3. Se houver erro de sintaxe, o Firebase mostra em vermelho

## Commits

Código atualizado em:
- `app/src/main/java/br/jss/motoreviso/managers/FirebaseManager.java`

Regras a atualizar em:
- Firebase Console → Storage → Rules
