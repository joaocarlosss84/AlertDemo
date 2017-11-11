//declara as variaveis do sistema
var ws = null,
    tags_lidas_pelo_portal = [],    
    dados_datalogger = null,
    dados_temperatura = null,      
    //valors em ºC (celsius)
    temperatura_amostras = {
            resfriada : { minimo : 2, maximo : 8 },
            congeladas : { minimo : -20, maximo : 0 },
            ambiente : { minimo : 15, maximo : 25 }
    },

    prefixo_bolsa = 'B',
    prefixo_exame = 'E',
    prefixo_datalogger = 'D',

    tipo_mensagem = {
        success : 1,
        warning : 2,
        danger : 3,
        info : 4
    },

    tipo_bolsa = {
        congelada : 'C',
        resfriada : 'F',
        ambiente: 'B',
        sangue: 'A'       
    },

    tamanho_bolsa = {
        pequena :  '0', 
        media : '1',
        grande : '2', 
        unico : 'F'
    },
    //timeout
    timeout_padrao = 300,
    //loader
    div_feedback = $("#loader"),    
    message = $("#message");
    message.hide();

    nome_cabecalho_msg_status = 'status',
    nome_cabecalho_msg_tags_lidas = 'epcid_list',
    nome_cabecalho_msg_datalogger = 'container_list',
    nome_cabecalho_msg_temperatura = 'datalogger_temp';            

    //**********variaveis de testes**********
    var DEBUG = true;   
    var count = 0; 
    //**********fim das variaveis de testes**********

function imprimiGraficoArea(resultado, div) {

    var options = {
        title: 'Temperatura da caixa',
        titleTextStyle: {
          color: '#4a9dda',
          fontName: 'verdana',
          fontSize: 15
        },       
        backgroundColor: 'transparent',    
        hAxis: {
            title: 'Tempo (h)',
            minValue: 'auto', 
            maxValue: 'auto',
            titleTextStyle: {
                color: '#4a9dda',
                fontName: 'verdana'                     
            },
            textStyle:{
                fontName: 'verdana',
                fontSize: 10
            },
        },
        colors: ['#4a9dda'],
        pointSize: 5,      
        vAxis: {
            title: 'Temperatura (°C)', 
            minValue: 'auto', 
            maxValue: 'auto',
            titleTextStyle: {
                color: '#4a9dda',
                fontName: 'verdana'                
            },
            textStyle:{
                fontName: 'verdana',
                fontSize: 10
            },
            gridlines: {
                color: 'transparent'
            },
            baselineColor: 'transparent'
        },
        series:{
            0: {          
                color: '#4a9dda',            
                pointShape: 'circle'
            },
            1: {          
                color: '#d37319',
                areaOpacity: 0,
                pointShape: 'square'              
            },
            2: {          
                color: '#d37319',
                areaOpacity: 0,
                pointShape: 'square'               
            }
        },
        animation: {
            duration: timeout_padrao,
            easing: 'inAndOut',
            startup: true
        },
        legend:{
            position: 'none'
        },
        textStyle: {
            fontName: 'verdana',
            fontSize: 10,
            bold: true,
            italic: true,
            // The color of the text.
            color: '#4a9dda',
            // The color of the text outline.
            auraColor: '#4a9dda',
            // The transparency of the text.
            opacity: 0.8
        },
        explorer: { axis: 'horizontal' },
        areaOpacity: 0.1,   
        chartArea:{
            width:'85%',
            height:'50%'
        }
    };

    var data = google.visualization.arrayToDataTable(resultado);
    var chart = new google.visualization.AreaChart(div);
    chart.draw(data, options);
    div_feedback.hide();  
}

function imprimiGraficoPizza(resultado, div){

     var options = {       
       titleTextStyle: { 
         color: '#4a9dda',
         fontName: 'verdana',
         fontSize: 16,
         align: 'bottom'
       },
       //pieHole: 0.5,         
       pieSliceText: 'value',
       pieSliceTextStyle: { color:'#000000', fontName: 'verdana', fontSize: 15 },
       legend: 'label',
       backgroundColor: 'transparent',
       colors: ['#88be55','#d37319'],//'#88be55','#eecaa1'],
       is3D: false,
       pieHole: 0.5,                        
       enableInteractivity: true,
       //slices: { 0: {offset: 0.2} },
       animation: {
         duration: 1000,
         easing: 'inAndOut',
         startup: true
       },
       chartArea:{
           width:'50%',
           height:'75%'
       }
     };

    var data = google.visualization.arrayToDataTable(resultado.tabela);
    options.title = resultado.nome_bolsa;

    var chart = new google.visualization.PieChart(div);
    chart.draw(data, options);
    div_feedback.hide();  
}

function apresentamensagem(tipo_msg, texto, aparecer_sempre){
    message.find(".alert").text(texto);
    switch (tipo_msg){
        case tipo_mensagem.success:
            classes = 'alert-success';
        case tipo_mensagem.warning:
            classes = 'alert-warning';
        case tipo_mensagem.danger:
            classes = 'alert-danger';
        default:
            classes = 'alert-info';
            break;
    }

    message.find(".alert").removeClass("alert-success alert-warning alert-danger alert-info");
    message.find(".alert").addClass(classes);
    message.fadeIn(500, function(){
        if(!aparecer_sempre)
            message.fadeOut(timeout_padrao*10);
    });
}

$(document).ready(function(){
    
    localStorage.setItem("ultimo.datalogger", null);
    limite_temperatura_bolsa = null;  

    //init tooltips
    $('[data-toggle="tooltip"]').tooltip();
    
    try{
        // websockets        
        ws = new WebSocket('ws://' + document.domain + ':' + location.port + '/ws');        
    }catch(exception){
        apresentamensagem(tipo_mensagem.warning, exception);
    }
    
    ws.onopen = function(e){              
        apresentamensagem(tipo_mensagem.success, 'Conexão com o servidor realizada.', false);  
        if(DEBUG){ 
            console.log('aberta conexao');            
        }
    };
    
    ws.onclose = function(e){        
        if(DEBUG) console.log('conexao fechada');
        apresentamensagem(tipo_mensagem.warning, 'Conexão com o servidor perdida.', true);        
    };
    
    ws.onerror = function(e){
        if(DEBUG) console.log('erro '+ e);
        
        apresentamensagem(tipo_mensagem.warning, e, false);
    };

    ws.onmessage = function (e) {
        //parse dos dados JSON para javascript object
        var msg = JSON.parse(e.data);        

        if(DEBUG)
        {                        
            if (msg.hasOwnProperty(nome_cabecalho_msg_tags_lidas)){
                div_feedback.hide();  
                //attribui os dados que veio do servidor na lista de tags corrente                
                $.each(msg.epcid_list, function(indice_epcid, epcid){
                    if(tags_lidas_pelo_portal.indexOf(epcid) == -1){
                        tags_lidas_pelo_portal = tags_lidas_pelo_portal.concat(epcid);                                                
                        
                        count++;           

                        if($(".allcharts").find("div[id='contador']").length == 0)
                        { 
                            var div = $("<div id='contador' style='float: left;' />");
                            div.text("contador: " + count);
                            $(".allcharts").append(div);
                        }else{
                            $("#contador").text("contador: " + count);
                        }

                        $(".allcharts").append("<label style='float: left; width: 100%;'>"+epcid+"</label>");
                    }
                });            
            }
            
            return;
        }             
        //lista com as tags lidas      
        if (msg.hasOwnProperty(nome_cabecalho_msg_tags_lidas)){
            //attribui os dados que veio do servidor na lista de tags corrente
            $.each(msg.epcid_list, function(indice_epcid, epcid){
                if(tags_lidas_pelo_portal.indexOf(epcid) == -1)
                    tags_lidas_pelo_portal = tags_lidas_pelo_portal.concat(epcid);
            });                       
        }        
        //existe datalogger
        if (msg.hasOwnProperty(nome_cabecalho_msg_datalogger)){
            
            if(msg.container_list == null | msg.container_list.length == 0)
            { 
                console.log('não existe datalogger.'); 
                return; 
            }              
                            
            //armazena as informações do datalogger
            dados_datalogger = msg.container_list[0];                        

            if(dados_datalogger.epcid != localStorage.getItem("ultimo.datalogger") && localStorage.getItem("ultimo.datalogger") != "null")
            {
                $("#temp_chart").children().fadeOut(timeout_padrao, function(){ $(this).remove() });
                $("#allcharts").children().fadeOut(timeout_padrao, function(){ $(this).remove() });
                localStorage.setItem("ultimo.datalogger", "null");
                div_feedback.show();
                //limpa os dados da caixa anterior
                tags_lidas_pelo_portal = [];
                dados_datalogger = null;                
                dados_temperatura = null;
            }         
            //se o datalogger já foi lido
            else{
                localStorage.setItem("ultimo.datalogger", dados_datalogger.epcid);
                //lê todos os dataloggers
                $.each(dados_datalogger.content,function(indice_bolsa, bolsa){  
                    //caso não exista bag              
                    if(bolsa.content == null | bolsa.content == 0)
                    { 
                        console.log('não existe bag.'); 
                        return; 
                    }      
                    var resultado = {};                
                        
                    switch (bolsa.type)
                    {
                        case prefixo_bolsa+tipo_bolsa.ambiente+tamanho_bolsa.pequena:
                        case prefixo_bolsa+tipo_bolsa.ambiente+tamanho_bolsa.media:
                        case prefixo_bolsa+tipo_bolsa.ambiente+tamanho_bolsa.grande:
                        case prefixo_bolsa+tipo_bolsa.ambiente+tamanho_bolsa.unico:
                            limite_temperatura_bolsa = temperatura_amostras.ambiente;
                        case prefixo_bolsa+tipo_bolsa.congelada+tamanho_bolsa.pequena:
                        case prefixo_bolsa+tipo_bolsa.congelada+tamanho_bolsa.media:
                        case prefixo_bolsa+tipo_bolsa.congelada+tamanho_bolsa.grande:
                        case prefixo_bolsa+tipo_bolsa.congelada+tamanho_bolsa.unico:
                            limite_temperatura_bolsa = temperatura_amostras.congeladas;
                        case prefixo_bolsa+tipo_bolsa.resfriada+tamanho_bolsa.pequena:
                        case prefixo_bolsa+tipo_bolsa.resfriada+tamanho_bolsa.media:
                        case prefixo_bolsa+tipo_bolsa.resfriada+tamanho_bolsa.grande:
                        case prefixo_bolsa+tipo_bolsa.resfriada+tamanho_bolsa.unico:
                            limite_temperatura_bolsa = temperatura_amostras.resfriada;
                        default:
                            limite_temperatura_bolsa = temperatura_amostras.ambiente;
                    }

                    resultado.nome_bolsa = bolsa.typeDesc;
                    resultado.id = bolsa.epcid;                    
                    //inicializa o cabeçalho para atender os padrões do DataTable da google
                    resultado.tabela = new Array(3);
                    resultado.tabela[0] = new Array(2);
                    resultado.tabela[1] = new Array(2);
                    resultado.tabela[2] = new Array(2);
                    resultado.tabela[0][0] = "quantidade";
                    resultado.tabela[0][1] = "total";
                    resultado.tabela[1][0] = "Lidas";
                    resultado.tabela[2][0] = 'Não Lidas';                    
                    resultado.tabela[1][1] = 0;
                    resultado.tabela[2][1] = 0;
                    //lê todos os exames
                    $.each(bolsa.content, function(indice_exame, exame){
                        if(tags_lidas_pelo_portal.indexOf(exame.epcid) == -1)
                            //tags que faltaram
                            resultado.tabela[2][1]++;                            
                        else
                            //tags que chegaram
                            resultado.tabela[1][1]++;
                    });                    
                    var novo_chart = null;
                    //verifica se o pie chart já existe
                    if($("#"+ resultado.id).length > 0){
                        $("#"+ resultado.id).remove();                                                
                    }

                    novo_chart = document.createElement("div");
                    $(".allcharts").append(novo_chart);                    
                    novo_chart.id = resultado.id;      
                    //imprimi o pie chart com os dados passados 
                    imprimiGraficoPizza(resultado, novo_chart);
                });                                    
            }      
        }

        //existe temperatura - só pode ser lida uma por caixa
        if(msg.hasOwnProperty(nome_cabecalho_msg_temperatura) || dados_temperatura != null){                                             
            //cria a pseudo-tabela que será utilizada pela api do google
            if(dados_temperatura == null)
                dados_temperatura = msg.datalogger_temp;
                
            var tabela = new Array(dados_temperatura.temp.length + 1);     
            var cabecalho_tabela = new Array(4);
            cabecalho_tabela[0] = 'tempo';
            cabecalho_tabela[1] = 'temperatura';
            cabecalho_tabela[2] = 'minimo';
            cabecalho_tabela[3] = 'máximo';
            tabela[0] = cabecalho_tabela;
            //faz o loop em cada temperatura para criar uma linha na tabela
            $.each(msg.datalogger_temp.temp, function(indice_temperatura, temperatura){
                //cada passo deve ser preenchido com o periodo 
                var linha = new Array(4);
                var data = new Date(msg.datalogger_temp.init_time.$date + (
                                        (msg.datalogger_temp.period*indice_temperatura)*1000
                                    ));
                //formata a data para hora:minuto:segundos
                linha[0] = ("00" + data.getHours()).slice(-2) + "h" + 
                        ("00" + data.getMinutes()).slice(-2) + "m" + 
                        ("00" + data.getSeconds()).slice(-2) + "s";
                linha[1] = temperatura;
                //inseri os dados de temperatura maxima e minima
                linha[2] = limite_temperatura_bolsa == null ? 0 : limite_temperatura_bolsa.minimo;
                linha[3] = limite_temperatura_bolsa == null ? 2 :  limite_temperatura_bolsa.maximo;
                tabela[++indice_temperatura] = linha;
            });
            //imprimi o grafico de temperatura
            imprimiGraficoArea(tabela, $("#temp_chart")[0]);
        }   
    };
});  