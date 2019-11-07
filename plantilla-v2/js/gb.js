import * as Gb from './gbapi.js'

function createGroupItem(group) {
  const html = [
    '<li id="grp_',
    group.name,
    '" ',
    'class="list-group-item d-flex justify-content-between align-items-center">',
    group.name,
    '<span class="badge badge-primary badge-pill" title="',
    group.elements.join(' '),
    '">',
    group.elements.length,
    '</span>',
    '</li>'
  ];
  return $(html.join(''));
}

function createVmItem(params) {
  const stateToBadge = {
    start: 'success',
    stop: 'danger',
    suspend: 'secondary',
    reset: 'warning'
  }
  const html = [
    '<li id="vm_',
    params.name,
    '" ',
    'class="list-group-item d-flex justify-content-between align-items-center">',
    params.name,
    '<span class="badge badge-',
    stateToBadge[params.state],
    ' badge-pill estado">&nbsp;</span>',
    '</li>'
  ];
  return $(html.join(''));
}

//
//
// Código de pegamento, ejecutado sólo una vez que la interfaz esté cargada.
// Generalmente de la forma $("selector").comportamiento(...)
//
//
$(function() { 
  
  // funcion de actualización de ejemplo. Llámala para refrescar interfaz
  function update(result) {
    try {
      // vaciamos un contenedor
      $("#grupos").empty();
      // y lo volvemos a rellenar con su nuevo contenido
      Gb.globalState.classes.forEach(group =>  $("#grupos").append(createGroupItem(group)));      
      // y asi para cada cosa que pueda haber cambiado
    } catch (e) {
      console.log('Error actualizando', e);
    }
  }

  // expone Gb para que esté accesible desde la consola
  window.Gb = Gb;
  const U = Gb.Util;

  // genera datos de ejemplo
  let classIds = ["1A", "1B", "2A", "2B", "3A", "3B"];
  let userIds = [];
  classIds.forEach(cid => {
    let teacher = U.randomUser(Gb.UserRoles.TEACHER, [cid]);
    Gb.addUser(teacher);
    userIds.push(teacher.uid);

    let students = U.fill(U.randomInRange(15,20), () => U.randomStudent(cid));

    students.forEach(s => {
      Gb.addStudent(s);           

      let parents = U.fill(U.randomInRange(1,2), 
        () => U.randomUser(Gb.UserRoles.GUARDIAN, [cid], [s.sid]));
      parents.forEach( p => {
        s.guardians.push(p.uid);
        userIds.push(p.uid);
        Gb.addUser(p);
      });      
    });

    Gb.addClass(new Gb.EClass(cid, [teacher], students));
  });
  Gb.addUser(U.randomUser(Gb.UserRoles.ADMIN));
  console.log(userIds);
  U.fill(30, () => U.randomMessage(userIds)).forEach( 
    m => Gb.send(m)
  );

  // muestra un mensaje de bienvenida
  console.log("online!", JSON.stringify(Gb.globalState, null, 2));
});
