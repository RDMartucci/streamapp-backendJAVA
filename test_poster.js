const http = require('http');

function login() {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify({ username: 'admin', password: 'admin1234' });
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, (res) => {
      let data = '';
      res.on('data', d => data += d);
      res.on('end', () => {
        try {
          const token = JSON.parse(res.data).token;
          console.log('Token:', token);
          testPoster(token);
        } catch (e) {
          console.error(e);
        }
      });
    });
    req.on('error', e => console.error('Login error:', e.message));
    req.write(JSON.stringify({ username: 'admin', password: 'admin1234' }));
    req.end();
  });
}

function testPoster(token) {
  const postData = JSON.stringify({
    path: 'D:/media/peliculas/Greenland 2 (2020).mkv',
    posterUrl: 'https://image.tmdb.org/t/p/w342/dPTa7jSIeaDhie2d9JSZr7qI0Tf.jpg',
    year: '2020',
    genres: 'Action,Thriller',
    voteAverage: '6.5',
    mediaType: 'movie'
  });
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: '/api/poster/by-path',
    method: 'PUT',
    headers: {
      'Authorization': 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzg3OTYwODU1LCJleHAiOjE3ODc1MjU5Mzl9.6MRw38_FAn0dMNyixsJnHM7LLv-jkxM2b2twxKRv_cB8',
      'Content-Type': 'application/json'
    },
    method: 'PUT'
  });
  const req = require('http').request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/poster/by-path',
    method: 'PUT',
    headers: {
      'Authorization': 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzg3OTYwODU1LCJleHAiOjE3ODc1MjU5Mzl9.6MRw38_FAn0dMNyixsJnHM7LLv-jkxM2b2twxKRv_cB8',
      'Content-Type': 'application/json'
    },
    method: 'PUT'
  }, (res) => {
    let data = '';
    res.on('data', d => data += d);
    res.on('end', () => {
      console.log('Status:', res.statusCode);
      console.log('Headers:', res.headers);
      console.log('Body:', data);
    });
  });
  req.on('error', e => console.error('Error:', e.message));
  req.write(JSON.stringify({
    path: 'D:/media/peliculas/Greenland 2 (2020).mkv',
    posterUrl: 'https://image.tmdb.org/t/p/w342/dPTa7jSIeaDhie2d9JSZr7qI0Tf.jpg',
    year: '2020',
    genres: 'Action,Thriller',
    voteAverage: '6.5',
    mediaType: 'movie'
  }));
  req.end();
}

require('http').request({
  hostname: 'localhost',
  port: 8080,
  path: '/api/auth/login',
  method: 'POST',
  headers: { 'Content-Type': 'application/json' }
}, (res) => {
  let data = '';
  res.on('data', d => data += d);
  res.on('end', () => {
    try {
      const token = JSON.parse(res.data).token;
      console.log('Token:', token);
      testPoster(token);
    } catch (e) {
      console.error(e);
    })
}).on('error', e => console.error(e)).end(JSON.stringify({ username: 'admin', password: 'admin1234' }));