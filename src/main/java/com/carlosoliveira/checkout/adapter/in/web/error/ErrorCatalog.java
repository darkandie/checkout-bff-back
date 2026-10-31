package com.carlosoliveira.checkout.adapter.in.web.error;

import com.carlosoliveira.checkout.adapter.in.web.error.FrontendErrorResponse.Button;
import com.carlosoliveira.checkout.adapter.in.web.error.FrontendErrorResponse.FrontendError;
import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public enum ErrorCatalog {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, DisplayType.BLOCK, "P-0001",
            "Não foi possível continuar",
            "Algumas informações enviadas estão incompletas ou inválidas. Revise os dados e tente novamente.",
            Buttons.RETRY, Buttons.BACK_TO_PAYMENT),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, DisplayType.BLOCK, "P-0002",
            "Página não encontrada",
            "O recurso solicitado não existe.",
            Buttons.BACK_TO_PLANS, null),

    METHOD_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, DisplayType.BLOCK, "P-0003",
            "Operação não suportada",
            "Esta operação não está disponível.",
            Buttons.BACK_TO_PLANS, null),

    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, DisplayType.BLOCK, "P-0004",
            "Formato não suportado",
            "O formato da requisição não é suportado.",
            Buttons.RETRY, null),

    PAYMENT_DECLINED(HttpStatus.UNPROCESSABLE_ENTITY, DisplayType.BLOCK, "P-1001",
            "Pagamento não aprovado",
            "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.",
            Buttons.RETRY, Buttons.CHANGE_PAYMENT_METHOD),

    PRODUCT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, DisplayType.BLOCK, "P-2001",
            "Produto indisponível",
            "Um ou mais produtos escolhidos não estão disponíveis. Escolha outro plano para continuar.",
            Buttons.BACK_TO_PLANS, null),

    CART_NOT_FOUND(HttpStatus.NOT_FOUND, DisplayType.BLOCK, "P-3001",
            "Carrinho não encontrado",
            "Não encontramos o seu carrinho. Escolha um plano para começar uma nova compra.",
            Buttons.BACK_TO_PLANS, null),

    CART_EXPIRED(HttpStatus.GONE, DisplayType.BLOCK, "P-3002",
            "Carrinho expirado",
            "O tempo para concluir esta compra acabou. Escolha o plano novamente para continuar.",
            Buttons.BACK_TO_PLANS, null),

    PAYMENT_METHOD_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, DisplayType.INTERVENTION, "P-4001",
            "Meio de pagamento indisponível",
            "O meio de pagamento escolhido não está disponível para esta compra. Escolha outra opção.",
            Buttons.CHANGE_PAYMENT_METHOD, null),

    INVALID_PAYMENT_DATA(HttpStatus.UNPROCESSABLE_ENTITY, DisplayType.INTERVENTION, "P-4002",
            "Dados de pagamento inválidos",
            "Confira os dados de pagamento informados e tente novamente.",
            Buttons.REVIEW_PAYMENT_DATA, Buttons.CHANGE_PAYMENT_METHOD),

    PAYMENT_ALREADY_APPROVED(HttpStatus.CONFLICT, DisplayType.INTERVENTION, "P-5001",
            "Pagamento já aprovado",
            "Esta compra já foi paga. Nenhuma nova cobrança foi realizada.",
            Buttons.SEE_CONFIRMATION, null),

    PAYMENT_IN_PROGRESS(HttpStatus.CONFLICT, DisplayType.INTERVENTION, "P-5002",
            "Pagamento em processamento",
            "Já estamos processando o pagamento desta compra. Aguarde alguns instantes.",
            Buttons.RETRY, null),

    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, DisplayType.BLOCK, "P-9001",
            "Serviço indisponível",
            "Estamos com instabilidade no momento. Tente novamente em alguns instantes.",
            Buttons.RETRY, null),

    UNEXPECTED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, DisplayType.BLOCK, "P-9999",
            "Algo deu errado",
            "Não foi possível concluir a operação. Tente novamente em alguns instantes.",
            Buttons.RETRY, null);

    private final HttpStatus status;
    private final DisplayType displayType;
    private final String code;
    private final String title;
    private final String description;
    private final Button primaryButton;
    private final Button secondaryButton;

    ErrorCatalog(HttpStatus status,
                 DisplayType displayType,
                 String code,
                 String title,
                 String description,
                 Button primaryButton,
                 Button secondaryButton) {
        this.status = status;
        this.displayType = displayType;
        this.code = code;
        this.title = title;
        this.description = description;
        this.primaryButton = primaryButton;
        this.secondaryButton = secondaryButton;
    }

    public static ErrorCatalog from(ErrorCode errorCode) {
        return valueOf(errorCode.name());
    }

    public FrontendErrorResponse toResponse(UUID cartId) {
        return new FrontendErrorResponse(
                displayType,
                name(),
                cartId,
                new FrontendError(code, title, description, primaryButton, secondaryButton)
        );
    }

    public HttpStatus status() {
        return status;
    }

    private static final class Buttons {

        static final Button RETRY = new Button("Tentar novamente", "RETRY");
        static final Button CHANGE_PAYMENT_METHOD = new Button("Trocar meio de pagamento", "/checkout/pagamento");
        static final Button REVIEW_PAYMENT_DATA = new Button("Revisar dados", "/checkout/pagamento");
        static final Button BACK_TO_PAYMENT = new Button("Voltar ao pagamento", "/checkout/pagamento");
        static final Button BACK_TO_PLANS = new Button("Escolher plano", "/planos");
        static final Button SEE_CONFIRMATION = new Button("Ver confirmação", "/checkout/sucesso");

        private Buttons() {
        }
    }
}
