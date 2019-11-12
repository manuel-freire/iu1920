import * as Gb from './gbapi.js'

function createGroupItem(mensaje) {
  const rid = 'x_' + Math.floor(Math.random()*1000000);
  const hid = 'h_'+rid;
  const cid = 'c_'+rid;
  
  const html = [
    '<div class="card">',
    '<div class="card-header" id="', hid, '">',
    '  <h2 class="mb-0">',
    '    <button class="btn btn-link" type="button"',
        ' data-toggle="collapse" data-target="#', cid, '"',
    '      aria-expanded="true" aria-controls="', rid, '">',
    '<b class="msg mtitle">', mensaje.title, '</b>',
    '<div class="msg mdate"> Enviado el ', 
    new Intl.DateTimeFormat('es-ES').format(mensaje.date),
    ' por ', mensaje.from,
    '</div>',
    '    </button>',
    '  </h2>',
    '</div>',
    '',
    '<div id="', cid, '" class="collapse show" aria-labelledby="', hid,'" ',
       'data-parent="#accordionExample">',
    '  <div class="card-body msg">',
    mensaje.body,
    '  </div>',
    '</div>',
    '</div>'
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
  window.demo = function update(result) {
    try {
      // vaciamos un contenedor
      $("#accordionExample").empty();
      // y lo volvemos a rellenar con su nuevo contenido
      Gb.globalState.messages.forEach(m =>  $("#accordionExample").append(createGroupItem(m)));      
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

    Gb.addClass(new Gb.EClass(cid, students.map(s => s.sid), [teacher.uid]));
  });
  Gb.addUser(U.randomUser(Gb.UserRoles.ADMIN));
  console.log(userIds);
  U.fill(30, () => U.randomMessage(userIds)).forEach( 
    m => Gb.send(m)
  );

  // muestra un mensaje de bienvenida
  console.log("online!", JSON.stringify(Gb.globalState, null, 2));
});
