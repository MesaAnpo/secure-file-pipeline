import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from scanner.worker import scan_file

class DummyClamd:
    def instream(self, buff):
        return {'stream': ('OK', 'dummy')}


def test_scan_file_result(monkeypatch, tmp_path):
    # Patch ClamAV client to avoid network calls
    monkeypatch.setattr('scanner.worker.clamd.ClamdNetworkSocket', lambda host=None: DummyClamd())
    f = tmp_path / 'fakefile.txt'
    f.write_text('dummy')
    result = scan_file(str(f))
    assert result in ['CLEAN', 'INFECTED']
