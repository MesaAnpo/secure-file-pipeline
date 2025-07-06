import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from scanner.worker import scan_file

class DummyClamd:
    def scan(self, path):
        return {path: ('stream', 'OK')}


def test_scan_file_result(monkeypatch):
    # Patch ClamAV client to avoid network calls
    monkeypatch.setattr('scanner.worker.clamd.ClamdNetworkSocket', lambda host=None: DummyClamd())
    result = scan_file('/tmp/fakefile.txt')
    assert result in ['CLEAN', 'INFECTED']
