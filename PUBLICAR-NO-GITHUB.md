# Colocar o Commander Table no GitHub

Este pacote contém os fontes organizados da alpha 0.3. O envio pode ser feito
para um repositório novo; não é necessário fazer fork do Forge.

## Criar e enviar

1. Extraia o ZIP. Entre na pasta `commander-table`, onde está `package.json`.
2. Acesse https://github.com/new usando sua conta.
3. Use o nome `commander-table` e a descrição sugerida:
   **Mesa web multiplayer de Commander com regras e IAs do Forge.**
4. Escolha **Public** ou **Private**, conforme o público desejado.
5. Crie o repositório vazio: deixe desmarcada a criação de README, licença e
   gitignore. Esses arquivos já estão neste pacote.
6. Instale o Git, se necessário: https://git-scm.com/downloads . No Windows,
   abra o Git Bash na pasta `commander-table`.
7. Execute os comandos abaixo, substituindo `SEU_USUARIO` pelo nome da conta.

```sh
git init -b main
git add .
git commit -m "Import Commander Table alpha 0.3"
git remote add origin https://github.com/SEU_USUARIO/commander-table.git
git push -u origin main
```

Se o Git pedir sua identidade para o commit, configure seu nome e o e-mail que
você utiliza no GitHub, ou o endereço de privacidade fornecido pela plataforma:

```sh
git config user.name "Seu nome"
git config user.email "Seu email de commits"
```

Depois repita o commit e o push. Conclua a autenticação pela janela de login
oferecida pelo Git/Git Credential Manager; não coloque senhas em arquivos do
projeto.

## Disponibilizar o programa para jogar

O repositório contém os fontes. O ZIP executável da alpha 0.3 deve ser anexado
a uma Release:

1. No repositório, abra **Releases** e **Draft a new release**.
2. Use a tag `v0.3.0-alpha.3`, apontando para `main`.
3. Nome: **Commander Table alpha 0.3**.
4. Anexe `Commander-Table-alpha-0.3.zip`, o pacote executável de aproximadamente
   67 MB entregue anteriormente. Ele já inclui os fontes correspondentes.
5. Marque a versão como **pre-release** e publique quando desejar disponibilizá-la.

O ZIP `Commander-Table-GitHub-source.zip` é a pasta de fontes preparada para
importação; ele não contém o servidor compilado nem substitui o ZIP executável.

## Continuar trabalhando

Depois de modificar e verificar os arquivos:

```sh
git add .
git commit -m "Descreva a alteração"
git push
```

Guarde a pasta do projeto. O arquivo `.gitignore` exclui dependências, builds,
perfis locais, logs e arquivos de ambiente. A revisão do Forge permanece em
`FORGE_REVISION`; atualizá-la exige recompilar e verificar os cenários.

Hospedar o repositório no GitHub não executa o servidor Java. Para jogar com
amigos, continue iniciando o programa no PC e compartilhando o convite pelo
túnel, conforme `server/LEIA-ME.md`.

Documentação oficial:
- https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github
- https://docs.github.com/en/pull-requests/reference/forks
- https://docs.github.com/en/repositories/releasing-projects-on-github
