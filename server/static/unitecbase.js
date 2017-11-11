/**
 * Created by 20006030 on 21/07/2016.
 */

function Template(main_query, queries) {
    /* Creates an object that binds to an imported copy of an HTML Template.
     The template is found on the document using a CSS selector query, this
     query is specified by 'main_query' (e.g. '#my_template').
     The properties of the object and theirs bindings to the template are
     specified by the 'queries' object.

     For example, consider this HTML:
     <template id="my_template">
     <span class="name"></span>
     <span class="id"></span>
     </template>

     We can bind to it using:
     var my_obj = new Template('#my_template', {
     name: '.name',
     id: '.id'
     }

     Then we can use each property as a DOM object:
     my_obj.name.innerHTML = '<b>Hermes Pardini</b>';
     my_obj.id.textContent = '12345';

     Note that the CSS queries on the object 'queries' are executed only on
     the scope of the template found by the 'main_query' */
    var t = document.querySelector(main_query);
    var clone = document.importNode(t.content, true);
    Object.keys(queries).forEach(function(key) {
        this[key] = clone.querySelector(queries[key]);
    }, this);
    this._main = clone;
}

function populateCombo(combo, options) {
    $.each(options, function(index, sValue ) {
        combo.append($("<option />").val(index).text(sValue));
    });
}

function addBootstrapTableRow(table, object) {
    object.id = table.bootstrapTable('getData').length;

    table.bootstrapTable('insertRow', {
        index: object.id,
        row: object
    });
}


//Toggle Datalogger Details Accordions
$('body').on('click.collapse-next.data-api', '[data-toggle=collapse-next]', function() {

    //Change the icon plus/minus from clickable row
    var aI = $(this).find('i');
    $(aI[0]).toggleClass('glyphicon-plus glyphicon-minus');

    //Get the next row with details
    var nextTR = $(this).closest('tr').next('tr');

    //Get Datalogger DIV Details
    var aDiv = $(nextTR).find('div');
    var $div = $(aDiv[0]);

    //Expand or Collapse
    $div.collapse('toggle');
});