#!/usr/bin/env python
# -*- coding: utf-8 -*-
# -*- mode: python -*-
import os
from setuptools import setup, find_packages, Command


class CleanCommand(Command):
    """Custom clean command to tidy up the project root."""
    user_options = []

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def run(self):
        os.system('rm -vrf ./build ./dist **/*.pyc **/*.jar **/*.egg-info')

setup(name='dataio-cli-acceptance-test',
      version=1.0,
      package_dir={'': 'src'},
      packages=find_packages(where='src', exclude=[]),
      package_data={'': ['*.jar']},
      test_suite="",
      provides=['acceptance_test'],
      scripts=['src/acceptance_test/bin/dataio-cli-acctest',
               'src/acceptance_test/bin/dataio-cli-flow-component.jar'],
      url="http://dbc.dk",
      maintainer="ioscrum",
      maintainer_email="i-o-scrum@dbc.dk",
      zip_safe=False,
      cmdclass={'clean': CleanCommand})
