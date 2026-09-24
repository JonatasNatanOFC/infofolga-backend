import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    cenario_100_vus: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '15s', target: 0 },
      ],
    },
    cenario_1000_vus: {
      executor: 'ramping-vus',
      startVUs: 0,
      startTime: '50s',
      stages: [
        { duration: '45s', target: 1000 },
        { duration: '15s', target: 0 },
      ],
    },
    cenario_100000_vus: {
      executor: 'ramping-vus',
      startVUs: 0,
      startTime: '2m',
      stages: [
        { duration: '1m', target: 8000},
        { duration: '30s', target: 0 },
      ],
    },
  },
};

export default function () {
  const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';

  // Credenciais vêm do ambiente: k6 run -e GERENTE_CPF=... -e GERENTE_SENHA=... -e FUNC_CPF=... -e FUNC_SENHA=... teste-carga.js
  const usuarios = [
    { cpf: __ENV.GERENTE_CPF, senha: __ENV.GERENTE_SENHA, role: 'GERENTE' },
    { cpf: __ENV.FUNC_CPF, senha: __ENV.FUNC_SENHA, role: 'FUNCIONARIO' }
  ];

  const usuarioSorteado = usuarios[Math.floor(Math.random() * usuarios.length)];

  const payloadLogin = JSON.stringify({ cpf: usuarioSorteado.cpf, senha: usuarioSorteado.senha });
  const paramsPublic = { headers: { 'Content-Type': 'application/json' } };

  const resLogin = http.post(`${BASE_URL}/auth/login`, payloadLogin, paramsPublic);
  check(resLogin, { 'login 200': (r) => r.status === 200 });

  if (resLogin.status === 200) {
    const token = resLogin.json('token');
    const paramsAuth = { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } };

    const resMe = http.get(`${BASE_URL}/colaboradores/me`, paramsAuth);
    check(resMe, { 'rota /me 200': (r) => r.status === 200 });

    if (usuarioSorteado.role === 'GERENTE') {
      const resTodos = http.get(`${BASE_URL}/colaboradores`, paramsAuth);
      check(resTodos, { 'rota /colaboradores (Gerente) 200': (r) => r.status === 200 });
    } else {
      const resStats = http.get(`${BASE_URL}/colaboradores/me/stats`, paramsAuth);
      check(resStats, { 'rota /stats (Funcionario) 200': (r) => r.status === 200 });

      const resMinhas = http.get(`${BASE_URL}/solicitacoes/minhas`, paramsAuth);
      check(resMinhas, { 'rota /minhas (Funcionario) 200': (r) => r.status === 200 });
    }
  }

  sleep(1);
}