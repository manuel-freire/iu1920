"use strict"

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

  Gb.login("16337065G", "ZX05e").then(d => console.log("login ok!", d));
});

async function populate(classes, minStudents, maxStudents, minParents, maxParents, msgCount) {
      const U = Gb.Util;

      // genera datos de ejemplo
      let classIds = classes || ["1A", "1B", "2A", "2B", "3A", "3B"];
      let minStudentsInClass = minStudents || 10;
      let maxStudentsInClass = maxStudents || 20;
      let minParentsPerStudent = minParents || 1;
      let maxParentsPerStudent = maxParents || 3;
      let userIds = [];
      let tasks = [];

      classIds.forEach(cid => {
        tasks.push(() => Gb.addClass(new Gb.EClass(cid)));
        let teacher = U.randomUser(Gb.UserRoles.TEACHER, [cid]);
        userIds.push(teacher.uid);
        tasks.push(() => Gb.addUser(teacher));

        let students = U.fill(U.randomInRange(minStudentsInClass, maxStudentsInClass), () => U.randomStudent(cid));
        students.forEach(s => {
          tasks.push(() => Gb.addStudent(s));
          let parents = U.fill(U.randomInRange(minParentsPerStudent, maxParentsPerStudent),
            () => U.randomUser(Gb.UserRoles.GUARDIAN, [], [s.sid]));
          parents.forEach( p => {
            userIds.push(p.uid);
            tasks.push(() =>  Gb.addUser(p));
          });
        });
      });
      tasks.push(() => Gb.addUser(U.randomUser(Gb.UserRoles.ADMIN)));
      U.fill(msgCount, () => U.randomMessage(userIds)).forEach(m => tasks.push(() => Gb.send(m)));

      // los procesa en secuencia contra un servidor
      for (let t of tasks) {
        try {
            console.log("Starting a task ...");
            await t().then(console.log("task finished!"));
        } catch (e) {
            console.log("ABORTED DUE TO ", e);
        }
      }
}

window.populate = populate