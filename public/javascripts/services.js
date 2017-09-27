function deleteCheckedServices() {
    console.log("Deleting");
    var items = $('#available-services-table')
        .find('input:checked[name="checked-services"]')
        .map(parseCheckedServices)
        .get();
    var request = {
        kind: "List[ServiceFinder]",
        items: items
    };

     $.ajax({
        headers: {"Content-Type": "application/json"},
        url: jsRoutes.controllers.HomeController.deleteServices.url,
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

function parseCheckedServices() {
    var id = $(this).attr('id').split("_")[2];
    var namespace = $("#svc_namepsace_"+id).text().replace(/\s/g,'');
    var serviceName = $("#svc_name_"+id).text().replace(/\s/g,'');
    return {
        name: serviceName,
        namespace: namespace
    }
}

window.onload=function () {
    document.getElementById("btn-services-delete").addEventListener('click', deleteCheckedServices);
};
