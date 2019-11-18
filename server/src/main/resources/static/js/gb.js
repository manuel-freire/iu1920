"use strict"

import * as Gb from './gbapi.js'

/**
 * Librería de cliente para interaccionar con el servidor de Garabatos.
 * Prácticas de IU 2019-20
 *
 * Para las prácticas de IU, pon aquí (o en otros js externos incluidos desde tus .htmls) el código
 * necesario para añadir comportamientos a tus páginas. Recomiendo separar el fichero en 2 partes:
 * - funciones que pueden generar cachos de contenido a partir del modelo, pero que no
 *   tienen referencias directas a la página
 * - un bloque rodeado de $(() => { y } donde está el código de pegamento que asocia comportamientos
 *   de la parte anterior con elementos de la página.
 *
 * Fuera de las prácticas, lee la licencia: dice lo que puedes hacer con él, que es esencialmente
 * lo que quieras siempre y cuando no digas que lo escribiste tú o me persigas por haberlo escrito mal.
 */

//
// PARTE 1:
// Código de comportamiento, que sólo se llama desde consola (para probarlo) o desde la parte 2,
// en respuesta a algún evento.
//

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

// funcion para generar datos de ejemplo: clases, mensajes entre usuarios, ...
// se puede no-usar, o modificar libremente
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

//
// PARTE 2:
// Código de pegamento, ejecutado sólo una vez que la interfaz esté cargada.
// Generalmente de la forma $("selector").cosaQueSucede(...)
//
$(function() { 
  
  // funcion de actualización de ejemplo. Llámala para refrescar interfaz
  function update(result) {
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


  // Servidor a utilizar. También puedes lanzar tú el tuyo en local (instrucciones en Github)
  Gb.connect("http://localhost:8080/api/");

  // ejemplo de login
  Gb.login("HDY0IQ", "cMbwKQ").then(d => {
    if (d !== undefined) {
        const u = Gb.resolve("HDY0IQ");
        console.log("login ok!", u);
    } else {
        console.log("error en login");
    }
  });

  // ejemplo de crear una clase, una vez logeados
  Gb.addClass({cid: "1A"})

  // ejemplo de crear un usuario, una vez logueados como admin (los no-admin no pueden hacer eso)
  Gb.addUser({
	"uid": "18950946G",
	"first_name": "Elena",
	"last_name": "Enseña Enséñez",
	"type": "teacher",
	"tels": [ "141-456-781"],
	"password" : "axarW!3",
    "classes": [
        "1A"
    ]});

    Gb.send({
        msgid: 123,
        from: "18950946G",
        to: [ "18950946G" ],
        title: "test",
        body: "lots of html"
    });
});

// cosas que exponemos para usarlas desde la consola
window.populate = populate
window.Gb = Gb;


