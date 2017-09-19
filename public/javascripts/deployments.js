function showDeploymentCreator() {
   var form =  document.getElementById("deploymentForm");
   var deploymentCreator = document.getElementById("deploymentCreator");
   var submitButton = document.getElementById("submit-button");
   var button = document.getElementById("showDeploymentCreator");


    function createAvailableDeploymentsCheckBoxes(form) {
        var availableDeployments = [];
        availableDeployments[0] = "Elasticsearch 5.5.1";
        availableDeployments[1] = "Kibana 5.5.1";
        availableDeployments[2] = "Nginx";

        for (i in availableDeployments) {
            var elemDiv= document.createElement('div');
            elemDiv.className="availableAppsList";
            form.appendChild(elemDiv);

            var newCheckBox = document.createElement('input');
            newCheckBox.type = 'checkbox';
            newCheckBox.id = 'App' + i;
            newCheckBox.name="list[deployments]";
            newCheckBox.value = availableDeployments[i] ;
            elemDiv.appendChild(newCheckBox);

            var newLabel = document.createElement('label');
            newLabel.setAttribute('for', newCheckBox.id);
            newLabel.innerHTML = newCheckBox.value;
            newLabel.className="checkbox-label";
            elemDiv.appendChild(newLabel);

            var resourcesDiv = document.createElement('div');
            resourcesDiv.id=newCheckBox.id+'_resources';
            resourcesDiv.className="resourcesOuter";
            elemDiv.appendChild(resourcesDiv);
            resourcesDiv=createResoucesFields(resourcesDiv)
        }
        console.log("Created checkboxes for available apps")

    }

    if($(deploymentCreator).css("display")==='none' && $(deploymentCreator).css("visibility")==='hidden') {
        if(form.childElementCount===0){
            createAvailableDeploymentsCheckBoxes(form);
        }
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

function createResoucesFields(resourcesDiv) {
            // ###### Limit CPU #######

    var CPU_STEP = "0.05";
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
    limitCPU.setAttribute("max",4);
    limitCPU.setAttribute("step", CPU_STEP);
    limitCPU.value =0.3;
    limitCPU.id = 'App'+i+'limitCPU';
    limitCPU.name="limitCPU[]";
    limitCPU_div.appendChild(limitCPU);

    var limitCPU_text = document.createElement('input');
    limitCPU_text.type='number';
    limitCPU_text.value = limitCPU.value;
    limitCPU_text.setAttribute("min",0);
    limitCPU_text.setAttribute("step", CPU_STEP);
    limitCPU_text.id='App'+i+'limitCPU_text';
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
    limitRAM.value =512;
    limitRAM.id = 'App'+i+'limitRAM';
    limitRAM.name="limitRAM[]";
    limitRAM_div.appendChild(limitRAM);

    var limitRAM_text  = document.createElement('input');
    limitRAM_text.type='number';
    limitRAM_text.value = limitRAM.value;
    limitRAM_text.setAttribute("min",0);
    limitRAM_text.setAttribute("step", RAM_STEP);
    limitRAM_text.id='App'+i+'limitRAM_text';
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
    requestCPU.setAttribute("max",4);
    requestCPU.setAttribute("step", CPU_STEP);
    requestCPU.value =1;
    requestCPU.id = 'App'+i+'requestCPU';
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
    requestCPU_text.id='App'+i+'requestCPU_text';
    requestCPU_text.name="requestCPU_text[]";
    requestCPU_div.appendChild(requestCPU_text);

    //############## Request RAM ############
    var requestRAM = document.createElement('input');
    requestRAM.type = "range";
    requestRAM.setAttribute("min",0);
    requestRAM.setAttribute("max",4096);
    requestRAM.setAttribute("step", RAM_STEP);
    requestRAM.value =128;
    requestRAM.id = 'App'+i+'requestRAM';
    requestRAM.name="requestRAM[]";
    requestRAM_div.appendChild(requestRAM);

    var requestRAM_text  = document.createElement('input');
    requestRAM_text.type='number';
    requestRAM_text.value = requestRAM.value;
    requestRAM_text.setAttribute("min",0);
    requestRAM_text.setAttribute("step", RAM_STEP);
    requestRAM_text.id='App'+i+'requestRAM_text';
    requestRAM_text.name="requestRAM_text[]";
    requestRAM_div.appendChild(requestRAM_text);

      var requestRAM_label = document.createElement('label');
    requestRAM_label.setAttribute('for', requestRAM.id);
    requestRAM_label.innerHTML="Request memory (MB):";
    requestRAM_label.className="input-description";
    requestRAM_div.insertBefore(requestRAM_label,requestRAM);

    return resourcesDiv

}

function submitForm() {
 checkboxes = $('#deploymentForm')
     .find('input:checked[name="list[deployments]"]')
     .map(function () {
         var splitted=$(this).val().split(" ");
         var item = {};
         item ['name']=splitted[0].toLowerCase();
         item ['version'] = splitted[1]!=null ? splitted[1] : "latest";
         return item; })
     .get();

 console.log(checkboxes);
    return checkboxes
}

function switchVisibility(resources) {
    if (($(resources).css("display") === 'none' && $(resources).css("visibility") === 'hidden')) {
        $(resources).css("display","block");
        $(resources).css("visibility","visible");
        console.log($(resources).attr('id') + " visibility switched: on")
    }else {
        $(resources).css("display","none");
        $(resources).css("visibility","hidden");
        console.log($(resources).attr('id') + " visibility switched: off")
    }
}

function rangeResourcesOnChange() {
    var elemTextId = "#"+$(this).attr('id')+"_text";
    $(elemTextId).val($(this).val());
    console.log(elemTextId + " set to: "+$(this).val())

}

function appListSelectionOnChange() {
      var id = $(this).attr('id');
      var resources = $("#"+id + '_resources');
      switchVisibility(resources);
      console.log(resources)
}

function numberResourcesOnChange() {
    $(this).val(Math.max($(this).val(), $(this).attr('min')));
    var rangeId = $(this).attr('id').replace("_text", "");
    $("#"+rangeId).val($(this).val());
    console.log(console.log(rangeId + " set to: "+$(this).val()))
}

window.onload=function () {
    document.getElementById("showDeploymentCreator").addEventListener('click', showDeploymentCreator);
    document.getElementById("submit-button").addEventListener('click',submitForm);

    $(document).on('change', 'input[type="checkbox"]', appListSelectionOnChange);
    $(document).on('change', 'input[type="range"]', rangeResourcesOnChange);
    $(document).on('change', 'input[type="number"]', numberResourcesOnChange);
};

