# Deploy no Google Cloud

A API roda no Cloud Run, com PostgreSQL no Cloud SQL e imagens no Artifact Registry. Toda a infraestrutura está em Terraform (`infra/terraform`) e o deploy é feito pelo GitHub Actions, autenticado via Workload Identity Federation, sem chaves de service account.

```
GitHub (push na main)
  └── CI: testes, cobertura, build da imagem
        └── Deploy: imagem no Artifact Registry ─► nova revisão no Cloud Run ─► smoke test
                                                         │
                                                         ├── Cloud SQL (PostgreSQL 16) via Cloud SQL connector
                                                         └── Secret Manager (senha do banco)
```

## Pré-requisitos

- Projeto GCP com faturamento ativo
- `gcloud` autenticado com permissão de owner no projeto
- Terraform 1.6 ou superior
- `gh` (GitHub CLI) autenticado, para configurar as variáveis do repositório

## 1. Bucket do state do Terraform

```bash
export PROJECT_ID=meu-projeto
export REGION=southamerica-east1

gcloud storage buckets create "gs://${PROJECT_ID}-tfstate" \
  --project "$PROJECT_ID" --location "$REGION" --uniform-bucket-level-access
gcloud storage buckets update "gs://${PROJECT_ID}-tfstate" --versioning
```

## 2. Provisionar a infraestrutura

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init -backend-config="bucket=${PROJECT_ID}-tfstate"
terraform apply
```

Preencha `terraform.tfvars` com o projeto e o repositório (`usuario/checkout-bff`). Depois do primeiro `init`, commite o `.terraform.lock.hcl` gerado.

O Cloud Run nasce com uma imagem de exemplo do Google; a primeira execução do pipeline substitui pela aplicação.

## 3. Configurar o GitHub

```bash
gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_ARTIFACT_REGISTRY --body "$(terraform output -raw artifact_registry)"
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --body "$(terraform output -raw workload_identity_provider)"
gh variable set GCP_DEPLOYER_SERVICE_ACCOUNT --body "$(terraform output -raw deployer_service_account)"
```

Nenhum segredo é necessário: o GitHub troca o token OIDC do workflow por uma credencial temporária do GCP. O provider só aceita tokens deste repositório vindos da branch `main`.

Opcionalmente, crie o environment `production` em Settings > Environments e exija aprovação manual antes de cada deploy.

## 4. Deploy

Todo push na `main` com o CI verde dispara o workflow `Deploy`, que:

1. constrói a imagem com a tag do commit e envia ao Artifact Registry;
2. publica uma nova revisão no Cloud Run;
3. aguarda o readiness e executa um pré-checkout real como smoke test.

Também é possível disparar manualmente em Actions > Deploy > Run workflow.

A URL pública aparece no environment `production` e em `terraform output service_url`. O Swagger fica em `<url>/swagger-ui.html`.

## Decisões

- **Cloud SQL connector em vez de IP autorizado.** A instância não libera nenhuma rede; a aplicação conecta com a identidade da service account de runtime (`roles/cloudsql.client`) e TLS gerenciado pelo connector.
- **Senha no Secret Manager.** O Cloud Run lê a senha como variável de ambiente vinda do secret, então ela não aparece na configuração do serviço. A senha também fica no state do Terraform, por isso o bucket é privado e versionado. O próximo passo seria IAM database authentication, eliminando a senha.
- **Identidades separadas.** A service account de runtime só acessa o banco e o secret. A de deploy só envia imagens para este repositório, publica revisões deste serviço e atua como a de runtime.
- **Sem Redis na nuvem por padrão.** O Memorystore tem custo fixo alto para um ambiente de avaliação, e a aplicação funciona sem cache. Para ligar, defina `CACHE_TYPE=redis` e `REDIS_HOST` no serviço.
- **Escala a zero.** Sem tráfego, o Cloud Run não cobra por instâncias; o startup CPU boost reduz o cold start da JVM.

## Custos e limpeza

O custo fixo relevante é a instância `db-f1-micro` do Cloud SQL; Cloud Run e Artifact Registry ficam praticamente zerados com pouco tráfego. Configure um alerta de orçamento no projeto e, ao fim da avaliação:

```bash
cd infra/terraform
terraform destroy
```
