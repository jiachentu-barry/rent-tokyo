import unittest

from suumo_to_properties import normalize_address_key


class NormalizeAddressKeyTest(unittest.TestCase):
    def test_same_address_with_different_spacing_should_have_same_key(self):
        first = "東京都 渋谷区 恵比寿 1-2-3"
        second = "東京都　渋谷区恵比寿1-2-3"

        self.assertEqual(normalize_address_key(first), normalize_address_key(second))

    def test_empty_address_should_return_empty_key(self):
        self.assertEqual(normalize_address_key("   "), "")


if __name__ == "__main__":
    unittest.main()
