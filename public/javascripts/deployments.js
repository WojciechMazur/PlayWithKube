function showDeploymentCreator() {
   var form =  document.getElementById("deploymentForm");
   var deploymentCreator = document.getElementById("deploymentCreator");
   var submitButton = document.getElementById("submit-button");
   var button = document.getElementById("showDeploymentCreator");

    function getAvailableApps() {
        $.getJSON("/images", function (result) {
            console.log("Successful ajax getJson(/images)")
            createAvailableDeploymentsCheckBoxes(result.items)
        });
    }

    function createAvailableDeploymentsCheckBoxes(apps) {

        for (i in apps) {
            var elemDiv= document.createElement('div');
            elemDiv.className="availableAppsList";
            elemDiv.id="App"+i+"_root";
            form.appendChild(elemDiv);

            var appDiv=document.createElement('div');
            appDiv.id="App"+i+"_div";
            elemDiv.appendChild(appDiv);

            var newCheckBox = document.createElement('input');
            newCheckBox.type = 'checkbox';
            newCheckBox.id = 'App' + i;
            newCheckBox.name="listDeployments";
            newCheckBox.value = apps[i].name += (apps[i].version!=="latest") ? " " + apps[i].version : ""  ;
            appDiv.appendChild(newCheckBox);

            var newLabel = document.createElement('label');
            newLabel.setAttribute('for', newCheckBox.id);
            newLabel.innerHTML = newCheckBox.value;
            newLabel.className="checkbox-label";
            appDiv.appendChild(newLabel);

            var resourcesDiv = document.createElement('div');
            resourcesDiv.id=newCheckBox.id+'_resources';
            resourcesDiv.className="resourcesOuter";
            elemDiv.appendChild(resourcesDiv);
            resourcesDiv=createResoucesFields(resourcesDiv);

            var configDiv = document.createElement('div');
            configDiv.id=newCheckBox.id+'_config';
            configDiv.className="config";
            elemDiv.appendChild(configDiv);
            var specificConfig = createSpecificFields(configDiv, apps[i].name.split(" ")[0].toLowerCase(), newCheckBox.id);
            if(specificConfig!==undefined)
                configDiv=specificConfig;

            var defaultDiv= document.createElement('div');
            defaultDiv.className="defaultConfig";
            defaultDiv.id=newCheckBox.id+"_defaultConfig";
            configDiv.appendChild(defaultDiv);

            createDefaultConfigFields(defaultDiv, newCheckBox.id);

        }
        console.log("Created checkboxes for available apps")

    }
    if(form.childElementCount===0)
        getAvailableApps();

    if($(deploymentCreator).css("display")==='none' && $(deploymentCreator).css("visibility")==='hidden') {
        $(deploymentCreator).css("display", "block");
        $(deploymentCreator).css("visibility", "visible");
        button.textContent = 'Hide deployment creator';
        console.log("Deployment creator set to visible");
    }else {
        $(deploymentCreator).css("display", "none");
        $(deploymentCreator).css("visibility", "hidden");
        button.textContent = 'Show deployment creator';
        console.log("Deployment creator set to hidden");
    }
}

function createSpecificFields(configDiv, appName, naming){
    switch(appName){
        case "elasticsearch": return createElasticsearchFields(configDiv, naming);
            break;
        case "kibana": return createKibanaConfigFields(configDiv, naming);
            break;
        default: return undefined
    }
}

function createElasticsearchFields(configDiv, naming) {
    console.log(naming);

    var appConfigDiv=document.createElement('div');
    appConfigDiv.className="appConfig";
    appConfigDiv.id=naming+"_appConfig";
    configDiv.appendChild(appConfigDiv);

    var masterDiv= document.createElement('div');
    masterDiv.className="configInner";
    appConfigDiv.appendChild(masterDiv);

    var dataDiv= document.createElement('div');
    dataDiv.className="configInner";
    appConfigDiv.appendChild(dataDiv);

    var clientDiv= document.createElement('div');
    clientDiv.className="configInner";
    appConfigDiv.appendChild(clientDiv);

    var isProductionMode=createCheckbox("Production Mode", naming+"_isProductionMode", "config-checkbox");
    var isExposed=createCheckbox("Create services", naming+"_isExposed", "config-checkbox");
    $(isExposed).prop('checked', true);

    var isProductionModeLabel=createLabel($(isProductionMode).attr('id'),"Production Mode", "config-checkbox-label");
    var isExposedLabel=createLabel($(isExposed).attr('id'),"Create services", "config-checkbox-label");

    var isMaster=createCheckbox("Master", naming+"_isMaster", "config-checkbox");
    var isData=createCheckbox("Data", naming+"_isData", "config-checkbox");
    var isClient=createCheckbox("Client", naming+"_isClient", "config-checkbox");

    var isMasterLabel=createLabel($(isMaster).attr('id'),"Master", "config-checkbox-label");
    var isDataLabel=createLabel($(isData).attr('id'),"Data", "config-checkbox-label");
    var isClientLabel=createLabel($(isClient).attr('id'),"Client", "config-checkbox-label");

    var masterNodes=createNumberInput(naming+"_isMaster_nodes",0);
    var dataNodes=createNumberInput(naming+"_isData_nodes",0);
    var clientNodes=createNumberInput(naming+"_isClient_nodes",0);

    isProductionMode.addEventListener('change', function () {
        var elemId="#"+$(this).attr('id').split("_")[0];
            switchVisibility(elemId+"_appConfig");
            switchVisibility(elemId+"_defaultConfig");
    });

    var triggers=[isClient,isData, isMaster];
    triggers.forEach(function (node) {
        node.addEventListener('change', function () {
            var elemId="#"+$(this).attr('id')+"_nodes";
           (switchVisibility(elemId, "inline-block")) ?  $(elemId).val(1) :  $(elemId).val(0)

        }, false)
    });

    masterDiv.appendChild(isMaster);
    masterDiv.appendChild(isMasterLabel);
    masterDiv.appendChild(masterNodes);
    dataDiv.appendChild(isData);
    dataDiv.appendChild(isDataLabel);
    dataDiv.appendChild(dataNodes);
    clientDiv.appendChild(isClient);
    clientDiv.appendChild(isClientLabel);
    clientDiv.appendChild(clientNodes);

    var divId="#"+naming+"_div";
    $(divId).append(isProductionMode);
    $(divId).append(isProductionModeLabel);
    $(divId).append(isExposed);
    $(divId).append(isExposedLabel);

    return configDiv
}

function createKibanaConfigFields(configDiv, naming) {
    return configDiv
}

function createDefaultConfigFields(configDiv, naming) {
    var replicas=createNumberInput(naming+"_replicas",1);
    replicas.className="defaultConfig";
    var replicasLabel = createLabel(replicas.id, "Number of replicas", "defaultConfig");
    configDiv.appendChild(replicasLabel);
    configDiv.appendChild(replicas);

    return configDiv
}

function createResoucesFields(resourcesDiv) {
            // ###### Limit CPU #######

    var CPU_STEP = "50";
    var RAM_STEP = "32";
    var limitCPU_div = document.createElement('div');
    var limitRAM_div = document.createElement('div');
    var requestCPU_div = document.createElement('div');
    var requestRAM_div = document.createElement('div');

    limitCPU_div.className="resourcesInner";
    limitRAM_div.className="resourcesInner";
    requestCPU_div.className="resourcesInner";
    requestRAM_div.className="resourcesInner";

    resourcesDiv.appendChild(limitCPU_div);
    resourcesDiv.appendChild(limitRAM_div);
    resourcesDiv.appendChild(requestCPU_div);
    resourcesDiv.appendChild(requestRAM_div);

    var limitCPU = document.createElement('input');
    limitCPU.type = "range";
    limitCPU.setAttribute("min",0);
    limitCPU.setAttribute("max",4000);
    limitCPU.setAttribute("step", CPU_STEP);
    limitCPU.value =1000;
    limitCPU.id = 'App'+i+'_limitCPU';
    limitCPU.name="limitCPU[]";
    limitCPU_div.appendChild(limitCPU);

    var limitCPU_text = document.createElement('input');
    limitCPU_text.type='number';
    limitCPU_text.value = limitCPU.value;
    limitCPU_text.setAttribute("min",0);
    limitCPU_text.setAttribute("step", CPU_STEP);
    limitCPU_text.id='App'+i+'_limitCPU_text';
    limitCPU_text.name="limitCPU_text[]";
    limitCPU_div.appendChild(limitCPU_text);

    var limitCPU_label = document.createElement('label');
    limitCPU_label.setAttribute('for', limitCPU.id);
    limitCPU_label.innerHTML="Limit CPU (miliCores):";
    limitCPU_label.className="input-description";
    limitCPU_div.insertBefore(limitCPU_label, limitCPU);

    // ############ Limit RAM ##########
    var limitRAM = document.createElement('input');
    limitRAM.type = "range";
    limitRAM.setAttribute("min",0);
    limitRAM.setAttribute("max",4096);
    limitRAM.setAttribute("step", RAM_STEP);
    limitRAM.value =1024;
    limitRAM.id = 'App'+i+'_limitRAM';
    limitRAM.name="limitRAM[]";
    limitRAM_div.appendChild(limitRAM);

    var limitRAM_text  = document.createElement('input');
    limitRAM_text.type='number';
    limitRAM_text.value = limitRAM.value;
    limitRAM_text.setAttribute("min",0);
    limitRAM_text.setAttribute("step", RAM_STEP);
    limitRAM_text.id='App'+i+'_limitRAM_text';
    limitRAM_text.name="limitRAM_text[]";
    limitRAM_div.appendChild(limitRAM_text);

     var limitRAM_label = document.createElement('label');
    limitRAM_label.setAttribute('for', limitRAM.id);
    limitRAM_label.innerHTML="Limit memory (MB):";
    limitRAM_label.className="input-description";
    limitRAM_div.insertBefore(limitRAM_label, limitRAM);

    // ########### Request CPU ############
    var requestCPU = document.createElement('input');
    requestCPU.type = "range";
    requestCPU.setAttribute("min",0);
    requestCPU.setAttribute("max",4000);
    requestCPU.setAttribute("step", CPU_STEP);
    requestCPU.value =300;
    requestCPU.id = 'App'+i+'_requestCPU';
    requestCPU.name="requestCPU[]";
    requestCPU_div.appendChild(requestCPU);

     var requestCPU_label = document.createElement('label');
    requestCPU_label.setAttribute('for', requestCPU.id);
    requestCPU_label.innerHTML="Request CPU (miliCores):";
    requestCPU_label.className="input-description";
    requestCPU_div.insertBefore(requestCPU_label, requestCPU);

    var requestCPU_text  = document.createElement('input');
    requestCPU_text.type='number';
    requestCPU_text.value = requestCPU.value;
    requestCPU_text.setAttribute("min",0);
    requestCPU_text.setAttribute("step", CPU_STEP);
    requestCPU_text.id='App'+i+'_requestCPU_text';
    requestCPU_text.name="requestCPU_text[]";
    requestCPU_div.appendChild(requestCPU_text);

    //############## Request RAM ############
    var requestRAM = document.createElement('input');
    requestRAM.type = "range";
    requestRAM.setAttribute("min",0);
    requestRAM.setAttribute("max",4096);
    requestRAM.setAttribute("step", RAM_STEP);
    requestRAM.value =256;
    requestRAM.id = 'App'+i+'_requestRAM';
    requestRAM.name="requestRAM[]";
    requestRAM_div.appendChild(requestRAM);

    var requestRAM_text  = document.createElement('input');
    requestRAM_text.type='number';
    requestRAM_text.value = requestRAM.value;
    requestRAM_text.setAttribute("min",0);
    requestRAM_text.setAttribute("step", RAM_STEP);
    requestRAM_text.id='App'+i+'_requestRAM_text';
    requestRAM_text.name="requestRAM_text[]";
    requestRAM_div.appendChild(requestRAM_text);

      var requestRAM_label = document.createElement('label');
    requestRAM_label.setAttribute('for', requestRAM.id);
    requestRAM_label.innerHTML="Request memory (MB):";
    requestRAM_label.className="input-description";
    requestRAM_div.insertBefore(requestRAM_label,requestRAM);

    return resourcesDiv
}

function getCurrentNamespace() {
 var url = window.location.toString().split('/');
 var namespaceIndex = url.indexOf("namespaces");
 return (namespaceIndex!==-1) ? url[namespaceIndex+1] : "default";
}

function  parseDeploymentCreatorForm() {
    var namespace = getCurrentNamespace();
    var id = "#"+$(this).attr('id');
    var splitted = $(this).val().split(" ");
    var name=splitted[0];
    var version=splitted[1];
    return {
        name: name.toLowerCase(),
        namespace: namespace,
        version: version !== undefined ? version : "latest",
        resources: {
            limits: {
                memory: $(id + "_limitRAM").val()+"Mi",
                cpu:    $(id + "_limitCPU").val()+"m"
            },
            requests: {
                memory: $(id + "_requestRAM").val()+"Mi",
                cpu:    $(id + "_requestCPU").val()+"m"
            }
        },
        replicas: Number($(id+"_replicas").val()),
        applicationConfig : parseApplicationBasedConfig(id, splitted[0].toLowerCase())

    }
}

function parseApplicationBasedConfig(id, name){
    switch(name){
        case "elasticsearch": return parseElasticsearchConfig(id);
        break;
        default: return defaultConfig(id)
    }
}

function parseElasticsearchConfig(id){
    return {
        productionMode: $(id + "_isProductionMode").is(':checked').toString(),
        expose: $(id + "_isExposed").is(':checked').toString(),
        isMaster: $(id + "_isMaster").is(':checked').toString(),
        masterNodes: $(id + "_isMaster_nodes").val(),
        isData: $(id + "_isData").is(':checked').toString(),
        dataNodes: $(id + "_isData_nodes").val(),
        isClient: $(id + "_isClient").is(':checked').toString(),
        clientNodes: $(id + "_isClient_nodes").val()
    }
}

function defaultConfig() {
    return {}
}

function submitForm() {

    var deploymentsList = {
        kind: "listDeployments",
        items: $('#deploymentForm')
        .find('input:checked[name="listDeployments"]')
        .map(parseDeploymentCreatorForm)
        .get()
    };

    $.ajax({
        headers: {"Content-Type": "application/json"},
        url: jsRoutes.controllers.HomeController.createDeployment.url,
        type: "POST",
        data: JSON.stringify(deploymentsList),
        dataType: "json",
        success: function (data) {
            console.log(data);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
            console.log(textStatus + " " + errorThrown)
        }
    });
    showDeploymentCreator()
}

function switchVisibility(resources, display, visible) {
    if (display===undefined) display="block";
    if (visible===undefined) visible="visible";
    if (($(resources).css("display") === 'none' && $(resources).css("visibility") === 'hidden')) {
        $(resources).css("display",display);
        $(resources).css("visibility",visible);
        console.log($(resources).attr('id') + " visibility switched: on");
        return true
    }else {
        $(resources).css("display","none");
        $(resources).css("visibility","hidden");
        console.log($(resources).attr('id') + " visibility switched: off")
        return false;
    }
}

function rangeResourcesOnChange() {
    var elemTextId = "#"+$(this).attr('id')+"_text";
    $(elemTextId).val($(this).val());
    console.log(elemTextId + " set to: "+$(this).val())
}

function appListSelectionOnChange() {
      var id = $(this).attr('id');
      switchVisibility("#"+id+"_isProductionMode_label");
      switchVisibility("#"+id+"_isExposed_label");
      switchVisibility("#"+id+"_resources");
      switchVisibility("#"+id+"_config");
      switchVisibility("#"+id+"_defaultConfig");
}

function numberResourcesOnChange() {
    $(this).val(Math.max($(this).val(), $(this).attr('min')));
    var rangeId = $(this).attr('id').replace("_text", "");
    $("#"+rangeId).val($(this).val());
    console.log(console.log(rangeId + " set to: "+$(this).val()))
}


function deleteCheckedDeployments() {
    console.log("Deleting");
    var items = $('#available-deployments-table')
        .find('input:checked[name="checked-deployments"]')
        .map(parseCheckedDeployments)
        .get();
    var request = {
        kind: "List[DeploymentFinder]",
        items: items
    };

     $.ajax({
        headers: {"Content-Type": "application/json"},
        url: jsRoutes.controllers.HomeController.deleteDeployments.url,
        type: "DELETE",
        data: JSON.stringify(request),
        dataType: "json",
        success: function (data) {
            console.log(data);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
            console.log(textStatus + " " + errorThrown)
        }
    });
    window.location.href = window.location.href;
}

function parseCheckedDeployments() {
    var id = $(this).attr('id').split("_")[2];
    var namespace = $("#deployment_namespace_"+id).text().replace(/\s/g,'');
    var serviceName = $("#deployment_name_"+id).text().replace(/\s/g,'');
    return {
        name: serviceName,
        namespace: namespace
    }
}

window.onload=function () {

};


function createCheckbox(value, id, className) {
    var checkbox =document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.id = id;
    checkbox.value = value;
    checkbox.className=className;
    return checkbox
}
function createLabel(forID, value, className) {
    var newLabel = document.createElement('label');
    newLabel.setAttribute('for', forID);
    newLabel.setAttribute('id', forID+"_label");
    newLabel.innerHTML = value;
    newLabel.className = className;
    return newLabel
}

function createNumberInput(id, value, min, max, step) {
    if (value === undefined) value = 0;
    if (min === undefined) min = 0;
    if (max === undefined) max = 100;
    if (step === undefined) step = 1;

    var numberField  = document.createElement('input');
    numberField.type='number';
    numberField.value = value;
    numberField.setAttribute("min",min);
    if(max!==undefined)
        numberField.setAttribute("max", max);
    numberField.setAttribute("step", step);
    numberField.id=id;
    return numberField
}

window.onload=function () {
    document.getElementById("showDeploymentCreator").addEventListener('click', showDeploymentCreator);
    document.getElementById("submit-button").addEventListener('click',submitForm);
    document.getElementById("btn-deployments-delete").addEventListener('click', deleteCheckedDeployments);

    $(document).on('change', 'input[type="checkbox"][name="listDeployments"]', appListSelectionOnChange);
    $(document).on('change', '.resourcesInner input[type="range"]', rangeResourcesOnChange);
    $(document).on('change', '.resourcesInner input[type="number"]', numberResourcesOnChange);
    };