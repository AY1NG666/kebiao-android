const http = require('http');
const fs = require('fs');
const path = require('path');

const DIR = '/opt/kebiao-updates';
const PORT = 8888;

http.createServer((req, res) => {
  const url = req.url === '/' ? '/version.json' : req.url;
  const filePath = path.join(DIR, url);

  // Security: prevent directory traversal
  if (!filePath.startsWith(DIR)) {
    res.writeHead(403);
    return res.end('Forbidden');
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      return res.end('Not found');
    }

    const ext = path.extname(filePath);
    const types = {
      '.json': 'application/json',
      '.apk': 'application/vnd.android.package-archive',
    };
    res.writeHead(200, {
      'Content-Type': types[ext] || 'application/octet-stream',
      'Access-Control-Allow-Origin': '*',
    });
    res.end(data);
  });
}).listen(PORT, () => {
  console.log(`kebiao-update server running on port ${PORT}`);
});
