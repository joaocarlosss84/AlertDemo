package net.unitecgroup.www.unitecrfid;

/**
 * Created by natan.grando on 31/07/2015.
 */
public class ServerResponse {

    //region Atributos Privados

    private boolean webResponseSuccessful;
    private String webResponseErrorMessage;
    private boolean consultSuccess;
    private String consultFailureMessage;

    //endregion

    //region Construtor

    public ServerResponse(boolean webResponseSuccessful, String webResponseErrorMessage, boolean consultSuccess, String consultFailureMessage) {
        this.webResponseSuccessful = webResponseSuccessful;
        this.webResponseErrorMessage = webResponseErrorMessage;
        this.consultSuccess = consultSuccess;
        this.consultFailureMessage = consultFailureMessage;
    }

    //endregion

    //region Propriedades

    public boolean isWebResponseSuccessful() {
        return webResponseSuccessful;
    }

    public String getWebResponseErrorMessage() {
        return webResponseErrorMessage;
    }

    public boolean isConsultSuccess() {
        return consultSuccess;
    }

    public String getConsultFailureMessage() {
        return consultFailureMessage;
    }

    //endregion
}
